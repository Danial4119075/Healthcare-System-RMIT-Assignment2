package healthcare.model;

/**
 * Doctor class that extends Staff.
 * Doctors can write prescriptions and check patient details.
 * Inherits isRosteredNow() from Staff, so roster checks work for doctors.
 */
public class Doctor extends Staff {
    private String specialization;

    public Doctor(String id, String name, String email, String phone, String username, String password, String specialization) {
        super(id, name, email, phone, username, password, "Doctor");
        this.specialization = specialization;
    }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    @Override
    public boolean canPerformAction(String action) {
        switch (action.toLowerCase()) {
            case "check_patient":
            case "add_prescription":
                return true;
            case "administer_medication":
            case "move_patient":
                return false;
            default:
                return false;
        }
    }

    // No need to override isRosteredNow() - inherited from Staff

    @Override
    public String toString() {
        return "Doctor{id='" + id + "', name='" + name + "', specialization='" + specialization + "'}";
    }
}
