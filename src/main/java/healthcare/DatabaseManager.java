package healthcare.database;

import healthcare.model.*;
import healthcare.utils.AuditLogger;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager class handles JDBC connections and database operations.
 * Implements data archiving for audit purposes as required by regulations.
 * Singleton pattern to ensure single database connection instance.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/healthcare.db";
    private static DatabaseManager instance;
    private Connection connection;

    // Package-private constructor (allows CareHome to instantiate)
    DatabaseManager() {
        initializeDatabase();
    }

    /**
     * Get singleton instance of DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize database and create necessary tables
     */
    private void initializeDatabase() {
        try {
            // Create data directory if it doesn't exist
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database initialized successfully");
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create database tables for audit and archival purposes
     */
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Audit log table
        String auditTable = "CREATE TABLE IF NOT EXISTS audit_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "staff_id TEXT NOT NULL, " +
                "action TEXT NOT NULL, " +
                "details TEXT, " +
                "timestamp TEXT NOT NULL)";
        stmt.execute(auditTable);

        // Patient archive table (main patient info on discharge)
        String patientArchive = "CREATE TABLE IF NOT EXISTS discharged_patients (" +
                "patient_id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "email TEXT, " +
                "phone TEXT, " +
                "date_of_birth TEXT, " +
                "gender TEXT, " +
                "age INTEGER, " +
                "medical_condition TEXT, " +
                "requires_isolation INTEGER, " +
                "bed_id TEXT, " +
                "admission_date TEXT, " +
                "discharge_date TEXT NOT NULL, " +
                "discharge_reason TEXT, " +
                "discharge_notes TEXT, " +
                "discharged_by TEXT NOT NULL)";
        stmt.execute(patientArchive);

        // Prescription archive table
        String prescriptionArchive = "CREATE TABLE IF NOT EXISTS archived_prescriptions (" +
                "prescription_id TEXT PRIMARY KEY, " +
                "patient_id TEXT NOT NULL, " +
                "doctor_id TEXT NOT NULL, " +
                "prescription_date TEXT NOT NULL, " +
                "notes TEXT, " +
                "archived_date TEXT NOT NULL, " +
                "FOREIGN KEY (patient_id) REFERENCES discharged_patients(patient_id))";
        stmt.execute(prescriptionArchive);

        // Individual medications from prescriptions
        String medicationArchive = "CREATE TABLE IF NOT EXISTS archived_medications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "prescription_id TEXT NOT NULL, " +
                "medication_name TEXT NOT NULL, " +
                "dosage TEXT NOT NULL, " +
                "frequency TEXT NOT NULL, " +
                "administration_time TEXT, " +
                "FOREIGN KEY (prescription_id) REFERENCES archived_prescriptions(prescription_id))";
        stmt.execute(medicationArchive);

        // Medication administration records (nurse actions)
        String medicationRecordArchive = "CREATE TABLE IF NOT EXISTS archived_medication_records (" +
                "record_id TEXT PRIMARY KEY, " +
                "patient_id TEXT NOT NULL, " +
                "nurse_id TEXT NOT NULL, " +
                "medication_name TEXT NOT NULL, " +
                "dosage_given TEXT NOT NULL, " +
                "administration_time TEXT NOT NULL, " +
                "administered INTEGER NOT NULL, " +
                "notes TEXT, " +
                "archived_date TEXT NOT NULL, " +
                "FOREIGN KEY (patient_id) REFERENCES discharged_patients(patient_id))";
        stmt.execute(medicationRecordArchive);

        stmt.close();
        System.out.println("Database tables created successfully");
    }

    /**
     * Archive complete patient data when discharged (Assignment Requirement)
     * Archives patient info, prescriptions, medications, and administration records
     */
    public void archiveDischargedPatient(Patient patient, String dischargeReason,
                                         String dischargeNotes, String dischargedBy) {
        try {
            String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // 1. Archive patient basic information
            String patientSQL = "INSERT INTO discharged_patients " +
                    "(patient_id, name, email, phone, date_of_birth, gender, age, " +
                    "medical_condition, requires_isolation, bed_id, admission_date, " +
                    "discharge_date, discharge_reason, discharge_notes, discharged_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(patientSQL)) {
                pstmt.setString(1, patient.getId());
                pstmt.setString(2, patient.getName());
                pstmt.setString(3, patient.getEmail());
                pstmt.setString(4, patient.getPhone());
                pstmt.setString(5, patient.getDateOfBirth().toString()); // ← FIXED: Convert LocalDate to String
                pstmt.setString(6, patient.getGender());
                pstmt.setInt(7, patient.getAge());
                pstmt.setString(8, patient.getMedicalCondition());
                pstmt.setInt(9, patient.requiresIsolation() ? 1 : 0);
                pstmt.setString(10, patient.getBedId());
                pstmt.setString(11, currentTimestamp); // Using current time as admission
                pstmt.setString(12, currentTimestamp);
                pstmt.setString(13, dischargeReason);
                pstmt.setString(14, dischargeNotes);
                pstmt.setString(15, dischargedBy);
                pstmt.executeUpdate();
            }

            // 2. Archive all prescriptions for this patient
            for (Prescription prescription : patient.getPrescriptions()) {
                archivePrescription(prescription, currentTimestamp);
            }

            // 3. Archive all medication administration records
            for (MedicationRecord record : patient.getMedicationHistory()) {
                archiveMedicationRecord(record, currentTimestamp);
            }

            System.out.println("Successfully archived data for patient: " + patient.getName() + " (ID: " + patient.getId() + ")");

        } catch (SQLException e) {
            System.err.println("Error archiving patient data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Archive prescription and its medications
     */
    private void archivePrescription(Prescription prescription, String archivedDate) throws SQLException {
        // Archive prescription header
        String sql = "INSERT INTO archived_prescriptions " +
                "(prescription_id, patient_id, doctor_id, prescription_date, notes, archived_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prescription.getPrescriptionId());
            pstmt.setString(2, prescription.getPatientId());
            pstmt.setString(3, prescription.getDoctorId());
            pstmt.setString(4, prescription.getPrescriptionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(5, prescription.getNotes());
            pstmt.setString(6, archivedDate);
            pstmt.executeUpdate();
        }

        // Archive individual medications
        for (Medication med : prescription.getMedications()) {
            archiveMedication(prescription.getPrescriptionId(), med);
        }
    }

    /**
     * Archive individual medication from prescription
     */
    private void archiveMedication(String prescriptionId, Medication medication) throws SQLException {
        String sql = "INSERT INTO archived_medications " +
                "(prescription_id, medication_name, dosage, frequency, administration_time) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prescriptionId);
            pstmt.setString(2, medication.getMedicationName());
            pstmt.setString(3, medication.getDosage());
            pstmt.setString(4, medication.getFrequency());
            pstmt.setString(5, medication.getAdministrationTime());
            pstmt.executeUpdate();
        }
    }

    /**
     * Archive medication administration record (nurse action)
     */
    private void archiveMedicationRecord(MedicationRecord record, String archivedDate) throws SQLException {
        String sql = "INSERT INTO archived_medication_records " +
                "(record_id, patient_id, nurse_id, medication_name, dosage_given, " +
                "administration_time, administered, notes, archived_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, record.getRecordId());
            pstmt.setString(2, record.getPatientId());
            pstmt.setString(3, record.getNurseId());
            pstmt.setString(4, record.getMedicationName());
            pstmt.setString(5, record.getDosageGiven());
            pstmt.setString(6, record.getAdministrationTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setInt(7, record.isAdministered() ? 1 : 0);
            pstmt.setString(8, record.getNotes());
            pstmt.setString(9, archivedDate);
            pstmt.executeUpdate();
        }
    }

    /**
     * Save audit log to database
     */
    public void saveAuditLog(AuditLogger.AuditRecord record) {
        String sql = "INSERT INTO audit_log (staff_id, action, details, timestamp) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, record.getStaffId());
            pstmt.setString(2, record.getAction());
            pstmt.setString(3, record.getDetails());
            pstmt.setString(4, record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving audit log: " + e.getMessage());
        }
    }

    /**
     * Get all audit records from database
     */
    public List<AuditLogger.AuditRecord> getAllAuditRecords() {
        List<AuditLogger.AuditRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY timestamp DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String staffId = rs.getString("staff_id");
                String action = rs.getString("action");
                String details = rs.getString("details");
                LocalDateTime timestamp = LocalDateTime.parse(
                        rs.getString("timestamp"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                records.add(new AuditLogger.AuditRecord(staffId, action, details, timestamp));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving audit records: " + e.getMessage());
        }

        return records;
    }

    /**
     * Get list of all discharged patients
     */
    public List<String> getArchivedPatients() {
        List<String> patients = new ArrayList<>();
        String sql = "SELECT patient_id, name, discharge_date, discharge_reason " +
                "FROM discharged_patients ORDER BY discharge_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String info = String.format("%s - %s (Discharged: %s - %s)",
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getString("discharge_date"),
                        rs.getString("discharge_reason")
                );
                patients.add(info);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving archived patients: " + e.getMessage());
        }

        return patients;
    }

    /**
     * Get complete discharge record for a patient (for audit purposes)
     */
    public String getDischargeReport(String patientId) {
        StringBuilder report = new StringBuilder();

        try {
            // Get patient info
            String patientSQL = "SELECT * FROM discharged_patients WHERE patient_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(patientSQL)) {
                pstmt.setString(1, patientId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    report.append("=== DISCHARGE REPORT ===\n");
                    report.append("Patient ID: ").append(rs.getString("patient_id")).append("\n");
                    report.append("Name: ").append(rs.getString("name")).append("\n");
                    report.append("Gender: ").append(rs.getString("gender")).append("\n");
                    report.append("Age: ").append(rs.getInt("age")).append("\n");
                    report.append("Medical Condition: ").append(rs.getString("medical_condition")).append("\n");
                    report.append("Discharge Date: ").append(rs.getString("discharge_date")).append("\n");
                    report.append("Discharge Reason: ").append(rs.getString("discharge_reason")).append("\n");
                    report.append("Discharged By: ").append(rs.getString("discharged_by")).append("\n\n");
                }
            }

            // Get prescriptions
            report.append("=== PRESCRIPTIONS ===\n");
            String prescSQL = "SELECT * FROM archived_prescriptions WHERE patient_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(prescSQL)) {
                pstmt.setString(1, patientId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    report.append("Prescription ID: ").append(rs.getString("prescription_id")).append("\n");
                    report.append("  Doctor ID: ").append(rs.getString("doctor_id")).append("\n");
                    report.append("  Date: ").append(rs.getString("prescription_date")).append("\n");
                    report.append("  Notes: ").append(rs.getString("notes")).append("\n\n");
                }
            }

            // Get medication records
            report.append("=== MEDICATION ADMINISTRATION RECORDS ===\n");
            String medSQL = "SELECT * FROM archived_medication_records WHERE patient_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(medSQL)) {
                pstmt.setString(1, patientId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    report.append("Record ID: ").append(rs.getString("record_id")).append("\n");
                    report.append("  Medication: ").append(rs.getString("medication_name")).append("\n");
                    report.append("  Dosage: ").append(rs.getString("dosage_given")).append("\n");
                    report.append("  Administered By: ").append(rs.getString("nurse_id")).append("\n");
                    report.append("  Time: ").append(rs.getString("administration_time")).append("\n");
                    report.append("  Status: ").append(rs.getInt("administered") == 1 ? "Administered" : "Pending").append("\n\n");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error generating discharge report: " + e.getMessage());
        }

        return report.toString();
    }

    /**
     * Close database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    /**
     * Test database connection
     */
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Get database connection (for advanced queries if needed)
     */
    protected Connection getConnection() {
        return connection;
    }
}
