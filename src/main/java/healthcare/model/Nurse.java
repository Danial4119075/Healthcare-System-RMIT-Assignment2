package healthcare.model;

/**
 * Nurse class that extends Staff.
 * Nurses can administer medications, move patients, and check patient details.
 * Inherits isRosteredNow() from Staff, so roster checks work for nurses.
 */
public class Nurse extends Staff {
    private String certification;

    public Nurse(String id, String name, String email, String phone, String username, String password, String certification) {
        super(id, name, email, phone, username, password, "Nurse");
        this.certification = certification;
    }

    public String getCertification() { return certification; }
    public void setCertification(String certification) { this.certification = certification; }

    @Override
    public boolean canPerformAction(String action) {
        switch (action.toLowerCase()) {
            case "check_patient":
            case "administer_medication":
            case "move_patient":
                return true;
            case "add_prescription":
                return false;
            default:
                return false;
        }
    }

    // No need to override isRosteredNow() - inherited from Staff

    @Override
    public String toString() {
        return "Nurse{id='" + id + "', name='" + name + "', certification='" + certification + "'}";
    }
}
