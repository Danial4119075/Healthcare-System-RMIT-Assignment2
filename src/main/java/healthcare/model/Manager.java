package healthcare.model;

/**
 * Manager class that extends Staff.
 * Managers can perform all administrative tasks including:
 * - Adding staff
 * - Adding/moving patients
 * - Discharging patients
 * - Managing shifts
 *
 * Managers CANNOT:
 * - Add prescriptions (Doctor only)
 * - Administer medication (Nurse only)
 */
public class Manager extends Staff {

    public Manager(String id, String name, String email, String phone, String username, String password) {
        super(id, name, email, phone, username, password, "Manager");
    }

    @Override
    public boolean canPerformAction(String action) {
        // Explicit manager permissions (positive check for clarity)
        switch (action.toLowerCase()) {
            case "add_staff":
            case "add_patient":
            case "discharge_patient":  // ← NEW: Explicitly allow discharge
            case "move_patient":       // Managers can move patients
            case "manage_shifts":
                return true;

            // Block actions reserved for Doctor/Nurse
            case "add_prescription":      // Doctor only
            case "administer_medication": // Nurse only
                return false;

            // Default: allow (managers have broad permissions)
            default:
                return true;
        }
    }

    @Override
    public boolean isRosteredNow() {
        // Managers are always considered on duty
        return true;
    }

    @Override
    public String toString() {
        return "Manager{id='" + id + "', name='" + name + "'}";
    }
}
