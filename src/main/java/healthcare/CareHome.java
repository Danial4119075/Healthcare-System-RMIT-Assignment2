package healthcare.model;

import healthcare.exceptions.*;
import healthcare.utils.*;
import healthcare.database.DatabaseManager;
import java.io.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CareHome class - Main business logic class for the healthcare system.
 * Implements Singleton pattern and manages all core functionality.
 * Demonstrates encapsulation, polymorphism, and composition.
 */
public class CareHome implements Serializable {
    private static CareHome instance;

    // Core data structures using generics
    private Map<String, Staff> staffMembers;
    private Map<String, Patient> patients;
    private List<Ward> wards;

    // Mark as transient so it is NOT serialized
    private transient AuditLogger auditLogger;
    private transient DatabaseManager databaseManager;

    // Data files
    private static final String DATA_FILE = "data/carehome_data.ser";
    private static final String BACKUP_FILE = "data/carehome_backup.ser";

    private CareHome() {
        initializeDataStructures();
        initializeWards();
        this.auditLogger = AuditLogger.getInstance();

        // Initialize database - creates healthcare.db file
        try {
            this.databaseManager = DatabaseManager.getInstance(); // ← FIXED: Use getInstance()
            System.out.println("✅ Database initialized successfully at data/healthcare.db");
        } catch (Exception e) {
            System.err.println("⚠ Warning: Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized CareHome getInstance() {
        if (instance == null) {
            instance = new CareHome();
        }
        return instance;
    }

    private void initializeDataStructures() {
        this.staffMembers = new HashMap<>();
        this.patients = new HashMap<>();
        this.wards = new ArrayList<>();
    }

    private void initializeWards() {
        Ward ward1 = new Ward("W1", "General Care Ward");
        ward1.addRoom(new Room("W1-R1", "W1", 4));
        ward1.addRoom(new Room("W1-R2", "W1", 2));
        ward1.addRoom(new Room("W1-R3", "W1", 1));
        ward1.addRoom(new Room("W1-R4", "W1", 3));
        ward1.addRoom(new Room("W1-R5", "W1", 2));
        ward1.addRoom(new Room("W1-R6", "W1", 4));
        wards.add(ward1);

        Ward ward2 = new Ward("W2", "Intensive Care Ward");
        ward2.addRoom(new Room("W2-R1", "W2", 3));
        ward2.addRoom(new Room("W2-R2", "W2", 1));
        ward2.addRoom(new Room("W2-R3", "W2", 4));
        ward2.addRoom(new Room("W2-R4", "W2", 2));
        ward2.addRoom(new Room("W2-R5", "W2", 1));
        ward2.addRoom(new Room("W2-R6", "W2", 3));
        wards.add(ward2);
    }

    // STAFF MANAGEMENT

    public void addStaff(Staff staff, String managerId) throws StaffNotAuthorizedException {
        Staff manager = staffMembers.get(managerId);
        if (manager == null || !manager.canPerformAction("add_staff")) {
            throw new StaffNotAuthorizedException(managerId, "add_staff", manager != null ? manager.getStaffType() : "Unknown");
        }
        staffMembers.put(staff.getId(), staff);
        auditLogger.logAction(managerId, "ADD_STAFF", "Added " + staff.getStaffType() + " " + staff.getName() + " (ID: " + staff.getId() + ")");
    }

    public Staff authenticateStaff(String username, String password) {
        return staffMembers.values().stream()
                .filter(staff -> staff.getUsername().equals(username) && staff.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public List<Staff> getStaffByType(String staffType) {
        return staffMembers.values().stream()
                .filter(staff -> staff.getStaffType().equals(staffType))
                .collect(Collectors.toList());
    }

    // PATIENT MANAGEMENT

    public void addPatient(Patient patient, String bedId, String staffId)
            throws BedOccupiedException, StaffNotAuthorizedException, StaffNotRosteredException {
        Staff staff = staffMembers.get(staffId);
        if (staff == null || !staff.canPerformAction("add_patient")) {
            throw new StaffNotAuthorizedException(staffId, "add_patient", staff != null ? staff.getStaffType() : "Unknown");
        }
        Bed bed = findBed(bedId);
        if (bed == null) {
            throw new IllegalArgumentException("Bed " + bedId + " not found");
        }
        Room foundRoom = null;
        outerloop:
        for (Ward ward : wards) {
            for (Room room : ward.getRooms()) {
                for (Bed b : room.getBeds()) {
                    if (b.getBedId().equals(bedId)) {
                        foundRoom = room;
                        break outerloop;
                    }
                }
            }
        }
        if (foundRoom == null) {
            throw new IllegalArgumentException("Room for bed " + bedId + " not found");
        }
        String newGender = patient.getGender();
        for (Bed b : foundRoom.getBeds()) {
            if (b.isOccupied()) {
                Patient p = patients.get(b.getPatientId());
                if (p != null && !p.getGender().equals(newGender)) {
                    throw new IllegalArgumentException(
                            "Cannot assign to room: only one gender allowed per room. Existing: " + p.getGender() + ", Attempted: " + newGender);
                }
            }
        }
        if (bed.isOccupied()) {
            throw new BedOccupiedException(bedId, bed.getPatientId(), patient.getId());
        }
        bed.assignPatient(patient.getId());
        patient.setBedId(bedId);
        patients.put(patient.getId(), patient);
        auditLogger.logAction(staffId, "ADD_PATIENT", "Added patient " + patient.getName() + " to bed " + bedId);
    }

    public void movePatient(String patientId, String newBedId, String nurseId)
            throws BedOccupiedException, StaffNotAuthorizedException, StaffNotRosteredException {
        Staff staff = staffMembers.get(nurseId);
        if (staff == null || !staff.canPerformAction("move_patient")) {
            throw new StaffNotAuthorizedException(nurseId, "move_patient", staff != null ? staff.getStaffType() : "Unknown");
        }
        // ROSTER CHECK for nurse
        if (!staff.isRosteredNow()) {
            throw new StaffNotRosteredException(staff.getId(), LocalDateTime.now(), "You are not rostered (scheduled) to work at this day/time.");
        }
        Patient patient = patients.get(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient " + patientId + " not found");
        }
        Bed currentBed = findBed(patient.getBedId());
        Bed newBed = findBed(newBedId);
        if (newBed == null) {
            throw new IllegalArgumentException("Bed " + newBedId + " not found");
        }
        Room foundRoom = null;
        outerloop:
        for (Ward ward : wards) {
            for (Room room : ward.getRooms()) {
                for (Bed b : room.getBeds()) {
                    if (b.getBedId().equals(newBedId)) {
                        foundRoom = room;
                        break outerloop;
                    }
                }
            }
        }
        if (foundRoom == null) {
            throw new IllegalArgumentException("Room for bed " + newBedId + " not found");
        }
        String newGender = patient.getGender();
        for (Bed b : foundRoom.getBeds()) {
            if (b.isOccupied()) {
                Patient p = patients.get(b.getPatientId());
                if (p != null && !p.getGender().equals(newGender)) {
                    throw new IllegalArgumentException(
                            "Cannot move patient to room: only one gender allowed per room. Existing: " + p.getGender() + ", Attempted: " + newGender);
                }
            }
        }
        if (newBed.isOccupied()) {
            throw new BedOccupiedException(newBedId, newBed.getPatientId(), patientId);
        }
        if (currentBed != null) {
            currentBed.vacateBed();
        }
        newBed.assignPatient(patientId);
        patient.setBedId(newBedId);
        auditLogger.logAction(nurseId, "MOVE_PATIENT", "Moved patient " + patient.getName() + " from " +
                (currentBed != null ? currentBed.getBedId() : "unknown") + " to " + newBedId);
    }

    // ═══════════════════════════════════════════════════════════════
    // NEW: DISCHARGE PATIENT (Assignment Requirement - Archive to DB)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Discharge a patient from the care home
     * Archives all patient data (prescriptions, medications, records) to database for audit purposes
     * @param patientId ID of patient to discharge
     * @param dischargeReason Reason for discharge
     * @param dischargeNotes Additional discharge notes
     * @param staffId ID of staff performing discharge (must be Manager)
     * @throws StaffNotAuthorizedException if staff is not a Manager
     * @throws IllegalArgumentException if patient not found
     */
    public void dischargePatient(String patientId, String dischargeReason, String dischargeNotes, String staffId)
            throws StaffNotAuthorizedException, IllegalArgumentException {

        // Check if staff exists and is a Manager
        Staff staff = staffMembers.get(staffId);
        if (staff == null) {
            throw new IllegalArgumentException("Staff not found: " + staffId);
        }

        if (!staff.canPerformAction("discharge_patient")) {
            throw new StaffNotAuthorizedException(
                    staffId,
                    "discharge_patient",
                    staff.getStaffType()
            );
        }

        // Find patient
        Patient patient = patients.get(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found: " + patientId);
        }

        String bedId = patient.getBedId();
        String patientName = patient.getName();

        // **ARCHIVE TO DATABASE FIRST** (before deletion) - Assignment requirement
        try {
            if (databaseManager != null) {
                databaseManager.archiveDischargedPatient(patient, dischargeReason, dischargeNotes, staffId);
                System.out.println("✅ Patient data archived to database");
            } else {
                System.err.println("⚠ Warning: DatabaseManager not initialized, data not archived");
            }
        } catch (Exception e) {
            System.err.println("⚠ Error archiving patient data: " + e.getMessage());
            e.printStackTrace();
            // Continue with discharge even if archiving fails (optional: throw exception instead)
        }

        // Free up the bed
        Bed bed = findBed(bedId);
        if (bed != null) {
            bed.vacateBed();
        }

        // Remove from active patients
        patients.remove(patientId);

        // Log the discharge
        auditLogger.logAction(staffId, "DISCHARGE_PATIENT",
                "Discharged patient " + patientName + " (ID: " + patientId + ") from bed " + bedId +
                        ". Reason: " + dischargeReason + ". Data archived to database for audit purposes.");

        System.out.println("✅ Patient " + patientName + " discharged successfully from bed " + bedId);
    }

    // PRESCRIPTION MANAGEMENT

    public void addPrescription(String patientId, Prescription prescription, String doctorId)
            throws StaffNotAuthorizedException, StaffNotRosteredException {
        Staff staff = staffMembers.get(doctorId);
        if (staff == null || !staff.canPerformAction("add_prescription")) {
            throw new StaffNotAuthorizedException(doctorId, "add_prescription", staff != null ? staff.getStaffType() : "Unknown");
        }
        // ROSTER CHECK for doctor
        if (!staff.isRosteredNow()) {
            throw new StaffNotRosteredException(staff.getId(), LocalDateTime.now(), "You are not rostered (scheduled) to work at this day/time.");
        }
        Patient patient = patients.get(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient " + patientId + " not found");
        }
        patient.addPrescription(prescription);
        auditLogger.logAction(doctorId, "ADD_PRESCRIPTION", "Added prescription " + prescription.getPrescriptionId() + " for patient " + patient.getName());
    }

    public void administerMedication(String patientId, MedicationRecord record, String nurseId)
            throws StaffNotAuthorizedException, StaffNotRosteredException {
        Staff staff = staffMembers.get(nurseId);
        if (staff == null || !staff.canPerformAction("administer_medication")) {
            throw new StaffNotAuthorizedException(nurseId, "administer_medication", staff != null ? staff.getStaffType() : "Unknown");
        }
        // ROSTER CHECK for nurse
        if (!staff.isRosteredNow()) {
            throw new StaffNotRosteredException(staff.getId(), LocalDateTime.now(), "You are not rostered (scheduled) to work at this day/time.");
        }
        Patient patient = patients.get(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient " + patientId + " not found");
        }
        record.setAdministered(true);
        patient.addMedicationRecord(record);
        auditLogger.logAction(nurseId, "ADMINISTER_MEDICATION", "Administered " + record.getMedicationName() + " to patient " + patient.getName());
    }

    // QUERY METHODS

    public Patient getPatient(String patientId) {
        return patients.get(patientId);
    }

    public Staff getStaff(String staffId) {
        return staffMembers.get(staffId);
    }

    public List<Ward> getWards() {
        return new ArrayList<>(wards);
    }

    public List<Patient> getAllPatients() {
        return new ArrayList<>(patients.values());
    }

    public List<Staff> getAllStaff() {
        return new ArrayList<>(staffMembers.values());
    }

    public Bed findBed(String bedId) {
        for (Ward ward : wards) {
            for (Bed bed : ward.getAllBeds()) {
                if (bed.getBedId().equals(bedId)) {
                    return bed;
                }
            }
        }
        return null;
    }

    public List<Bed> getAvailableBeds() {
        List<Bed> availableBeds = new ArrayList<>();
        for (Ward ward : wards) {
            for (Bed bed : ward.getAllBeds()) {
                if (!bed.isOccupied()) {
                    availableBeds.add(bed);
                }
            }
        }
        return availableBeds;
    }

    // SHIFT MANAGEMENT
    public void checkCompliance() throws IllegalStateException {
        for (Staff staff : staffMembers.values()) {
            if (staff instanceof Nurse) {
                int totalShifts = staff.getTotalWeeklyShifts();
                if (totalShifts != 7) {
                    throw new IllegalStateException("Nurse " + staff.getId() + " does not have exactly 7 shifts assigned.");
                }
                for (String day : Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
                    if (staff.getShiftsForDay(day).size() > 1) {
                        throw new IllegalStateException("Nurse " + staff.getId() + " has more than one shift on " + day);
                    }
                    int dailyHours = staff.getDailyHours(day);
                    if (dailyHours > 8) {
                        throw new IllegalStateException("Nurse " + staff.getId() + " exceeds 8 hours on " + day);
                    }
                }
            }
            if (staff instanceof Doctor) {
                int weeklyHours = staff.getTotalWeeklyHours(1);
                if (weeklyHours < 7) {
                    throw new IllegalStateException("Doctor " + staff.getId() + " does not have 7 hours assigned.");
                }
                for (String day : Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
                    if (staff.getShiftsForDay(day).size() < 1) {
                        throw new IllegalStateException("Doctor " + staff.getId() + " does not have a shift on " + day);
                    }
                }
            }
        }
    }

    // ---------------- NEW IMPROVED COMPLIANCE REPORT ----------------

    public String generateComplianceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔════════════════════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                        STAFF SHIFT COMPLIANCE REPORT                                       ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════════════════════════════════╝\n\n");

        // NURSES SECTION
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("NURSES - Required: 7 shifts per week (one 8-hour shift per day)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        for (Staff staff : staffMembers.values()) {
            if (staff instanceof Nurse) {
                String[] weekDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                String[] shortDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
                int totalShifts = 0;
                int totalHours = 0;
                boolean hasViolations = false;

                sb.append("┌─ ").append(staff.getName()).append(" (").append(staff.getId()).append(")\n");

                for (int i = 0; i < weekDays.length; i++) {
                    List<String> shifts = staff.getShiftsForDay(shortDays[i]);
                    int shiftCount = shifts != null ? shifts.size() : 0;
                    totalShifts += shiftCount;

                    sb.append("│  ").append(weekDays[i]).append(": ");

                    if (shiftCount == 0) {
                        sb.append("No shift assigned");
                    } else if (shiftCount == 1) {
                        sb.append(shifts.get(0)).append(" (8 hours)");
                        totalHours += 8;
                    } else {
                        sb.append("⚠ VIOLATION - Multiple shifts: ");
                        sb.append(String.join(", ", shifts));
                        hasViolations = true;
                        totalHours += (shiftCount * 8);
                    }
                    sb.append("\n");
                }

                // Check compliance
                boolean compliant = (totalShifts == 7) && !hasViolations;

                sb.append("│\n");
                sb.append("│  Total Shifts: ").append(totalShifts).append(" / 7 required\n");
                sb.append("│  Total Hours: ").append(totalHours).append(" hours\n");
                sb.append("│  Status: ");
                if (compliant) {
                    sb.append("✓ COMPLIANT\n");
                } else {
                    sb.append("✗ NON-COMPLIANT");
                    if (totalShifts != 7) {
                        sb.append(" (Wrong number of shifts)");
                    }
                    if (hasViolations) {
                        sb.append(" (Multiple shifts per day)");
                    }
                    sb.append("\n");
                }
                sb.append("└─────────────────────────────────────────────────────────────────────────────\n\n");
            }
        }

        // DOCTORS SECTION
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("DOCTORS - Required: Minimum 7 hours per week (at least 1 hour per day)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        for (Staff staff : staffMembers.values()) {
            if (staff instanceof Doctor) {
                String[] weekDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                String[] shortDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
                int totalHours = 0;
                int daysWorked = 0;

                sb.append("┌─ ").append(staff.getName()).append(" (").append(staff.getId()).append(")\n");

                for (int i = 0; i < weekDays.length; i++) {
                    List<String> shifts = staff.getShiftsForDay(shortDays[i]);
                    int shiftCount = shifts != null ? shifts.size() : 0;
                    totalHours += shiftCount;

                    sb.append("│  ").append(weekDays[i]).append(": ");

                    if (shiftCount == 0) {
                        sb.append("No shift assigned");
                    } else {
                        sb.append(shiftCount).append(" hour").append(shiftCount > 1 ? "s" : "");
                        daysWorked++;
                    }
                    sb.append("\n");
                }

                boolean compliant = (totalHours >= 7) && (daysWorked >= 7);

                sb.append("│\n");
                sb.append("│  Days Worked: ").append(daysWorked).append(" / 7 required\n");
                sb.append("│  Total Hours: ").append(totalHours).append(" / 7 minimum required\n");
                sb.append("│  Status: ");
                if (compliant) {
                    sb.append("✓ COMPLIANT\n");
                } else {
                    sb.append("✗ NON-COMPLIANT");
                    if (totalHours < 7) {
                        sb.append(" (Insufficient hours)");
                    }
                    if (daysWorked < 7) {
                        sb.append(" (Not working all 7 days)");
                    }
                    sb.append("\n");
                }
                sb.append("└─────────────────────────────────────────────────────────────────────────────\n\n");
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("End of Compliance Report\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        return sb.toString();
    }

    // DATA PERSISTENCE

    public void saveData() throws IOException {
        File dataFile = new File(DATA_FILE);
        if (dataFile.exists()) {
            dataFile.renameTo(new File(BACKUP_FILE));
        }
        new File("data").mkdirs();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        }
        auditLogger.logAction("SYSTEM", "SAVE_DATA", "Saved all system data");
    }

    public static CareHome loadData() throws IOException, ClassNotFoundException {
        File dataFile = new File(DATA_FILE);
        if (!dataFile.exists()) {
            return getInstance();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            instance = (CareHome) ois.readObject();
            instance.auditLogger = AuditLogger.getInstance();

            // Re-initialize database manager after deserialization
            try {
                instance.databaseManager = DatabaseManager.getInstance(); // ← FIXED: Use getInstance()
            } catch (Exception e) {
                System.err.println("⚠ Warning: Could not re-initialize database: " + e.getMessage());
            }

            return instance;
        }
    }

    public void createSampleData() {
        try {
            // ═══════════════════════════════════════════════════════════════
            // CREATE SAMPLE STAFF (only if no staff exists)
            // ═══════════════════════════════════════════════════════════════

            if (staffMembers.isEmpty()) {
                Manager manager = new Manager("MGR001", "John Manager", "manager@carehome.com", "0123456789", "admin", "admin123");
                staffMembers.put(manager.getId(), manager);

                Doctor doctor1 = new Doctor("DOC001", "Dr. Sarah Wilson", "sarah@carehome.com", "0123456788", "doctor1", "doc123", "General Medicine");
                Doctor doctor2 = new Doctor("DOC002", "Dr. Mike Johnson", "mike@carehome.com", "0123456787", "doctor2", "doc123", "Cardiology");
                staffMembers.put(doctor1.getId(), doctor1);
                staffMembers.put(doctor2.getId(), doctor2);

                Nurse nurse1 = new Nurse("NUR001", "Emma Thompson", "emma@carehome.com", "0123456786", "nurse1", "nur123", "RN");
                Nurse nurse2 = new Nurse("NUR002", "James Brown", "james@carehome.com", "0123456785", "nurse2", "nur123", "LPN");
                Nurse nurse3 = new Nurse("NUR003", "Lisa Davis", "lisa@carehome.com", "0123456784", "nurse3", "nur123", "RN");
                staffMembers.put(nurse1.getId(), nurse1);
                staffMembers.put(nurse2.getId(), nurse2);
                staffMembers.put(nurse3.getId(), nurse3);

                // Assign only ONE shift per day for nurses (as per requirement)
                for (Nurse nurse : Arrays.asList(nurse1, nurse2, nurse3)) {
                    for (String day : Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
                        nurse.assignShift(day, "8AM-4PM");  // One 8-hour shift per day
                    }
                }

                for (Doctor doctor : Arrays.asList(doctor1, doctor2)) {
                    for (String day : Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
                        doctor.assignShift(day, "1HR");
                    }
                }

                auditLogger.logAction("SYSTEM", "CREATE_SAMPLE_DATA", "Created sample staff and shift assignments");
                System.out.println("✅ Created sample staff");
            }

            // ═══════════════════════════════════════════════════════════════
            // CREATE SAMPLE PATIENTS (only if no patients exist)
            // ═══════════════════════════════════════════════════════════════

            if (patients.isEmpty()) {
                System.out.println("🏥 Creating 6 sample patients...");

                // Create sample patients with different medical conditions
                Patient patient1 = new Patient(
                        "PAT001",
                        "Alice Johnson",
                        "alice.j@email.com",
                        "0412345678",
                        LocalDate.of(1945, 5, 12),
                        "F",
                        "Hypertension",
                        false
                );

                Patient patient2 = new Patient(
                        "PAT002",
                        "Bob Smith",
                        "bob.s@email.com",
                        "0423456789",
                        LocalDate.of(1950, 8, 20),
                        "M",
                        "Diabetes Type 2",
                        false
                );

                Patient patient3 = new Patient(
                        "PAT003",
                        "Carol White",
                        "carol.w@email.com",
                        "0434567890",
                        LocalDate.of(1938, 3, 15),
                        "F",
                        "Pneumonia",
                        true  // Requires isolation
                );

                Patient patient4 = new Patient(
                        "PAT004",
                        "David Brown",
                        "david.b@email.com",
                        "0445678901",
                        LocalDate.of(1955, 11, 8),
                        "M",
                        "Heart Disease",
                        false
                );

                Patient patient5 = new Patient(
                        "PAT005",
                        "Emma Davis",
                        "emma.d@email.com",
                        "0456789012",
                        LocalDate.of(1942, 7, 25),
                        "F",
                        "Arthritis",
                        false
                );

                Patient patient6 = new Patient(
                        "PAT006",
                        "Frank Miller",
                        "frank.m@email.com",
                        "0467890123",
                        LocalDate.of(1948, 2, 18),
                        "M",
                        "COPD",
                        false
                );

                // ═══════════════════════════════════════════════════════════════
                // GENDER-SEGREGATED ROOM ASSIGNMENT (ONE GENDER PER ROOM)
                // ═══════════════════════════════════════════════════════════════

                // Patient 1 - W1-R1-B1 (Female only room)
                Bed bed1 = findBed("W1-R1-B1");
                if (bed1 != null && !bed1.isOccupied()) {
                    bed1.assignPatient("PAT001");
                    patient1.setBedId("W1-R1-B1");
                    patients.put("PAT001", patient1);
                    System.out.println("   ✓ Added Alice Johnson (F) to W1-R1-B1");
                }

                // Patient 2 - W1-R2-B1 (Male only room - DIFFERENT from W1-R1)
                Bed bed2 = findBed("W1-R2-B1");
                if (bed2 != null && !bed2.isOccupied()) {
                    bed2.assignPatient("PAT002");
                    patient2.setBedId("W1-R2-B1");
                    patients.put("PAT002", patient2);
                    System.out.println("   ✓ Added Bob Smith (M) to W1-R2-B1");
                }

                // Patient 3 - W1-R3-B1 (Female, isolation - single bed room)
                Bed bed3 = findBed("W1-R3-B1");
                if (bed3 != null && !bed3.isOccupied()) {
                    bed3.assignPatient("PAT003");
                    patient3.setBedId("W1-R3-B1");
                    patients.put("PAT003", patient3);
                    System.out.println("   ✓ Added Carol White (F) to W1-R3-B1 [ISOLATION]");
                }

                // Patient 4 - W1-R4-B1 (Male only room)
                Bed bed4 = findBed("W1-R4-B1");
                if (bed4 != null && !bed4.isOccupied()) {
                    bed4.assignPatient("PAT004");
                    patient4.setBedId("W1-R4-B1");
                    patients.put("PAT004", patient4);
                    System.out.println("   ✓ Added David Brown (M) to W1-R4-B1");
                }

                // Patient 5 - W1-R5-B1 (Female only room)
                Bed bed5 = findBed("W1-R5-B1");
                if (bed5 != null && !bed5.isOccupied()) {
                    bed5.assignPatient("PAT005");
                    patient5.setBedId("W1-R5-B1");
                    patients.put("PAT005", patient5);
                    System.out.println("   ✓ Added Emma Davis (F) to W1-R5-B1");
                }

                // Patient 6 - W2-R2-B1 (Male, isolation - single bed room)
                Bed bed6 = findBed("W2-R2-B1");
                if (bed6 != null && !bed6.isOccupied()) {
                    bed6.assignPatient("PAT006");
                    patient6.setBedId("W2-R2-B1");
                    patients.put("PAT006", patient6);
                    System.out.println("   ✓ Added Frank Miller (M) to W2-R2-B1 [ISOLATION]");
                }

                // Add sample prescriptions
                Prescription prescription1 = new Prescription("RX001", "PAT001", "DOC001", "Blood pressure management");
                prescription1.addMedication(new Medication("Amlodipine", "5mg", "Once daily", "08:00", "Take with water"));
                prescription1.addMedication(new Medication("Lisinopril", "10mg", "Once daily", "08:00", "Take in the morning"));
                patient1.addPrescription(prescription1);

                Prescription prescription2 = new Prescription("RX002", "PAT002", "DOC002", "Diabetes management");
                prescription2.addMedication(new Medication("Metformin", "500mg", "Twice daily", "08:00, 20:00", "Take with meals"));
                prescription2.addMedication(new Medication("Insulin", "20 units", "Before meals", "07:00, 12:00, 18:00", "Inject subcutaneously"));
                patient2.addPrescription(prescription2);

                Prescription prescription3 = new Prescription("RX003", "PAT003", "DOC001", "Antibiotic treatment");
                prescription3.addMedication(new Medication("Amoxicillin", "500mg", "Three times daily", "08:00, 14:00, 20:00", "Take with food"));
                patient3.addPrescription(prescription3);

                System.out.println("✅ Created 6 sample patients with prescriptions (gender-segregated rooms)!");
                auditLogger.logAction("SYSTEM", "CREATE_SAMPLE_PATIENTS",
                        "Created 6 sample patients with prescriptions in wards");
            }

        } catch (Exception e) {
            System.err.println("❌ Error creating sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
