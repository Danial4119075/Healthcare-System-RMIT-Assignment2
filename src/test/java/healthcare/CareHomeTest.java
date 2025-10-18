package healthcare;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import healthcare.model.*;
import healthcare.exceptions.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * JUnit tests for CareHome class.
 * Tests business rules and exception handling.
 */
public class CareHomeTest {
    private CareHome careHome;
    private Manager manager;
    private Doctor doctor;
    private Nurse nurse;
    private Patient patient;

    @BeforeEach
    void setUp() {
        careHome = CareHome.getInstance();

        // Create test staff
        manager = new Manager("MGR001", "Test Manager", "mgr@test.com", "1234567890", "testmgr", "pass123");
        doctor = new Doctor("DOC001", "Dr. Test", "doc@test.com", "1234567891", "testdoc", "pass123", "General");
        nurse = new Nurse("NUR001", "Test Nurse", "nurse@test.com", "1234567892", "testnur", "pass123", "RN");

        // Create test patient
        patient = new Patient("PAT001", "Test Patient", "pat@test.com", "1234567893", 
                            LocalDate.of(1980, 1, 1), "M", "Diabetes", false);

        try {
            // Add staff to care home
            careHome.addStaff(manager, manager.getId());
            careHome.addStaff(doctor, manager.getId());
            careHome.addStaff(nurse, manager.getId());
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test successful patient addition to vacant bed")
    void testAddPatientToVacantBed() {
        try {
            // Get a vacant bed
            List<Bed> availableBeds = careHome.getAvailableBeds();
            assertFalse(availableBeds.isEmpty(), "Should have available beds");

            String bedId = availableBeds.get(0).getBedId();

            // Add patient
            careHome.addPatient(patient, bedId, manager.getId());

            // Verify patient was added
            Patient addedPatient = careHome.getPatient(patient.getId());
            assertNotNull(addedPatient, "Patient should be added to system");
            assertEquals(bedId, addedPatient.getBedId(), "Patient should be assigned to correct bed");

            // Verify bed is now occupied
            Bed bed = careHome.findBed(bedId);
            assertTrue(bed.isOccupied(), "Bed should be occupied");
            assertEquals(patient.getId(), bed.getPatientId(), "Bed should contain correct patient");

        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test adding patient to occupied bed throws exception")
    void testAddPatientToOccupiedBed() {
        try {
            // Get a vacant bed and add first patient
            List<Bed> availableBeds = careHome.getAvailableBeds();
            String bedId = availableBeds.get(0).getBedId();
            careHome.addPatient(patient, bedId, manager.getId());

            // Create second patient
            Patient patient2 = new Patient("PAT002", "Test Patient 2", "pat2@test.com", "1234567894",
                                         LocalDate.of(1975, 5, 5), "F", "Hypertension", false);

            // Try to add second patient to same bed
            assertThrows(BedOccupiedException.class, () -> {
                careHome.addPatient(patient2, bedId, manager.getId());
            }, "Should throw BedOccupiedException when trying to add patient to occupied bed");

        } catch (Exception e) {
            fail("Unexpected exception during setup: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test unauthorized staff cannot add patients")
    void testUnauthorizedStaffCannotAddPatients() {
        List<Bed> availableBeds = careHome.getAvailableBeds();
        String bedId = availableBeds.get(0).getBedId();

        // Nurse should not be able to add patients
        assertThrows(StaffNotAuthorizedException.class, () -> {
            careHome.addPatient(patient, bedId, nurse.getId());
        }, "Nurses should not be authorized to add patients");

        // Doctor should not be able to add patients
        assertThrows(StaffNotAuthorizedException.class, () -> {
            careHome.addPatient(patient, bedId, doctor.getId());
        }, "Doctors should not be authorized to add patients");
    }

    @Test
    @DisplayName("Test doctor can add prescriptions")
    void testDoctorCanAddPrescriptions() {
        try {
            // First add patient to system
            List<Bed> availableBeds = careHome.getAvailableBeds();
            String bedId = availableBeds.get(0).getBedId();
            careHome.addPatient(patient, bedId, manager.getId());

            // Create prescription
            Prescription prescription = new Prescription("PRES001", patient.getId(), doctor.getId(), "Test prescription");
            Medication medication = new Medication("Aspirin", "100mg", "Once daily", "8:00", "Take with food");
            prescription.addMedication(medication);

            // Doctor should be able to add prescription
            assertDoesNotThrow(() -> {
                careHome.addPrescription(patient.getId(), prescription, doctor.getId());
            }, "Doctor should be able to add prescriptions");

            // Verify prescription was added
            Patient updatedPatient = careHome.getPatient(patient.getId());
            assertEquals(1, updatedPatient.getPrescriptions().size(), "Patient should have one prescription");

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test nurse cannot add prescriptions")
    void testNurseCannotAddPrescriptions() {
        try {
            // First add patient to system
            List<Bed> availableBeds = careHome.getAvailableBeds();
            String bedId = availableBeds.get(0).getBedId();
            careHome.addPatient(patient, bedId, manager.getId());

            // Create prescription
            Prescription prescription = new Prescription("PRES001", patient.getId(), nurse.getId(), "Test prescription");
            Medication medication = new Medication("Aspirin", "100mg", "Once daily", "8:00", "Take with food");
            prescription.addMedication(medication);

            // Nurse should not be able to add prescription
            assertThrows(StaffNotAuthorizedException.class, () -> {
                careHome.addPrescription(patient.getId(), prescription, nurse.getId());
            }, "Nurses should not be authorized to add prescriptions");

        } catch (Exception e) {
            fail("Unexpected exception during setup: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test nurse can administer medications")
    void testNurseCanAdministerMedications() {
        try {
            // First add patient to system
            List<Bed> availableBeds = careHome.getAvailableBeds();
            String bedId = availableBeds.get(0).getBedId();
            careHome.addPatient(patient, bedId, manager.getId());

            // Create medication record
            MedicationRecord record = new MedicationRecord("MED001", patient.getId(), nurse.getId(), "Aspirin", "100mg");

            // Nurse should be able to administer medication
            assertDoesNotThrow(() -> {
                careHome.administerMedication(patient.getId(), record, nurse.getId());
            }, "Nurse should be able to administer medications");

            // Verify medication record was added
            Patient updatedPatient = careHome.getPatient(patient.getId());
            assertEquals(1, updatedPatient.getMedicationHistory().size(), "Patient should have medication history");
            assertTrue(record.isAdministered(), "Medication should be marked as administered");

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test doctor cannot administer medications")
    void testDoctorCannotAdministerMedications() {
        try {
            // First add patient to system
            List<Bed> availableBeds = careHome.getAvailableBeds();
            String bedId = availableBeds.get(0).getBedId();
            careHome.addPatient(patient, bedId, manager.getId());

            // Create medication record
            MedicationRecord record = new MedicationRecord("MED001", patient.getId(), doctor.getId(), "Aspirin", "100mg");

            // Doctor should not be able to administer medication
            assertThrows(StaffNotAuthorizedException.class, () -> {
                careHome.administerMedication(patient.getId(), record, doctor.getId());
            }, "Doctors should not be authorized to administer medications");

        } catch (Exception e) {
            fail("Unexpected exception during setup: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test successful patient move between beds")
    void testPatientMoveSuccessful() {
        try {
            // First add patient to a bed
            List<Bed> availableBeds = careHome.getAvailableBeds();
            assertTrue(availableBeds.size() >= 2, "Need at least 2 available beds for this test");

            String originalBedId = availableBeds.get(0).getBedId();
            String newBedId = availableBeds.get(1).getBedId();

            careHome.addPatient(patient, originalBedId, manager.getId());

            // Move patient to new bed
            careHome.movePatient(patient.getId(), newBedId, nurse.getId());

            // Verify patient was moved
            Patient movedPatient = careHome.getPatient(patient.getId());
            assertEquals(newBedId, movedPatient.getBedId(), "Patient should be in new bed");

            // Verify original bed is now vacant
            Bed originalBed = careHome.findBed(originalBedId);
            assertFalse(originalBed.isOccupied(), "Original bed should be vacant");

            // Verify new bed is occupied
            Bed newBed = careHome.findBed(newBedId);
            assertTrue(newBed.isOccupied(), "New bed should be occupied");
            assertEquals(patient.getId(), newBed.getPatientId(), "New bed should contain correct patient");

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test staff authentication")
    void testStaffAuthentication() {
        // Test valid credentials
        Staff authenticatedStaff = careHome.authenticateStaff("testmgr", "pass123");
        assertNotNull(authenticatedStaff, "Should authenticate with valid credentials");
        assertEquals(manager.getId(), authenticatedStaff.getId(), "Should return correct staff member");

        // Test invalid credentials
        Staff notAuthenticated = careHome.authenticateStaff("testmgr", "wrongpass");
        assertNull(notAuthenticated, "Should not authenticate with invalid credentials");

        // Test non-existent user
        Staff nonExistent = careHome.authenticateStaff("nonexistent", "pass123");
        assertNull(nonExistent, "Should not authenticate non-existent user");
    }

    @Test
    @DisplayName("Test ward and bed initialization")
    void testWardAndBedInitialization() {
        List<Ward> wards = careHome.getWards();
        assertEquals(2, wards.size(), "Should have 2 wards");

        // Check total beds across both wards
        int totalBeds = wards.stream().mapToInt(Ward::getTotalBeds).sum();
        assertTrue(totalBeds > 0, "Should have beds available");

        // Check each ward has 6 rooms
        for (Ward ward : wards) {
            assertEquals(6, ward.getRooms().size(), "Each ward should have 6 rooms");
        }
    }
}
