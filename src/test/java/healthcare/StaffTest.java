package healthcare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import healthcare.model.*;

/**
 * JUnit tests for Staff hierarchy classes.
 */
public class StaffTest {

    @Test
    @DisplayName("Test Doctor permissions")
    void testDoctorPermissions() {
        Doctor doctor = new Doctor("DOC001", "Dr. Test", "doc@test.com", "1234567890",
                "testdoc", "pass123", "General Medicine");

        // Doctors can check patients and add prescriptions
        assertTrue(doctor.canPerformAction("check_patient"));
        assertTrue(doctor.canPerformAction("add_prescription"));

        // Doctors cannot administer medication or move patients
        assertFalse(doctor.canPerformAction("administer_medication"));
        assertFalse(doctor.canPerformAction("move_patient"));

        // Test unknown action
        assertFalse(doctor.canPerformAction("unknown_action"));
    }

    @Test
    @DisplayName("Test Nurse permissions")
    void testNursePermissions() {
        Nurse nurse = new Nurse("NUR001", "Test Nurse", "nurse@test.com", "1234567890",
                "testnur", "pass123", "RN");

        // Nurses can check patients, administer medication, and move patients
        assertTrue(nurse.canPerformAction("check_patient"));
        assertTrue(nurse.canPerformAction("administer_medication"));
        assertTrue(nurse.canPerformAction("move_patient"));

        // Nurses cannot add prescriptions
        assertFalse(nurse.canPerformAction("add_prescription"));

        // Test unknown action
        assertFalse(nurse.canPerformAction("unknown_action"));
    }

    @Test
    @DisplayName("Test Manager permissions")
    void testManagerPermissions() {
        Manager manager = new Manager("MGR001", "Test Manager", "mgr@test.com", "1234567890",
                "testmgr", "pass123");

        // Managers can perform all actions
        assertTrue(manager.canPerformAction("check_patient"));
        assertTrue(manager.canPerformAction("add_prescription"));
        assertTrue(manager.canPerformAction("administer_medication"));
        assertTrue(manager.canPerformAction("move_patient"));
        assertTrue(manager.canPerformAction("add_staff"));
        assertTrue(manager.canPerformAction("assign_shifts"));

        // Even unknown actions return true for managers
        assertTrue(manager.canPerformAction("unknown_action"));
    }

    @Test
    @DisplayName("Test Staff shift management")
    void testStaffShiftManagement() {
        Nurse nurse = new Nurse("NUR001", "Test Nurse", "nurse@test.com", "1234567890",
                "testnur", "pass123", "RN");

        // Initially no shifts for Monday
        assertTrue(nurse.getShiftsForDay("MON").isEmpty());

        // Add shifts for Monday
        nurse.assignShift("MON", "8AM-4PM");
        nurse.assignShift("MON", "2PM-10PM");

        assertEquals(2, nurse.getShiftsForDay("MON").size());
        assertTrue(nurse.getShiftsForDay("MON").contains("8AM-4PM"));
        assertTrue(nurse.getShiftsForDay("MON").contains("2PM-10PM"));

        // Adding duplicate shift should not increase count
        nurse.assignShift("MON", "8AM-4PM");
        assertEquals(2, nurse.getShiftsForDay("MON").size());

        // Remove shift
        nurse.removeShift("MON", "8AM-4PM");
        assertEquals(1, nurse.getShiftsForDay("MON").size());
        assertFalse(nurse.getShiftsForDay("MON").contains("8AM-4PM"));
    }

    @Test
    @DisplayName("Test Staff inheritance and polymorphism")
    void testStaffPolymorphism() {
        // Test polymorphism
        Staff doctor = new Doctor("DOC001", "Dr. Test", "doc@test.com", "1234567890",
                "testdoc", "pass123", "General Medicine");
        Staff nurse = new Nurse("NUR001", "Test Nurse", "nurse@test.com", "1234567890",
                "testnur", "pass123", "RN");
        Staff manager = new Manager("MGR001", "Test Manager", "mgr@test.com", "1234567890",
                "testmgr", "pass123");

        // All are Staff instances
        assertTrue(doctor instanceof Staff);
        assertTrue(nurse instanceof Staff);
        assertTrue(manager instanceof Staff);

        // All are Person instances (inheritance)
        assertTrue(doctor instanceof Person);
        assertTrue(nurse instanceof Person);
        assertTrue(manager instanceof Person);

        // Test staff types
        assertEquals("Doctor", doctor.getStaffType());
        assertEquals("Nurse", nurse.getStaffType());
        assertEquals("Manager", manager.getStaffType());

        // Test polymorphic behavior
        Staff[] staffArray = {doctor, nurse, manager};
        for (Staff staff : staffArray) {
            assertNotNull(staff.getName());
            assertNotNull(staff.getStaffType());
            // Each has different permissions (polymorphism)
            boolean canAddPrescription = staff.canPerformAction("add_prescription");
            if (staff instanceof Doctor || staff instanceof Manager) {
                assertTrue(canAddPrescription);
            } else {
                assertFalse(canAddPrescription);
            }
        }
    }
}
