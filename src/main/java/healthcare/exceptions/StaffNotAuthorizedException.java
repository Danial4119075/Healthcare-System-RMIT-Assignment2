package healthcare.exceptions;

/**
 * Exception thrown when staff member tries to perform an action they are not authorized for.
 * For example, if a nurse tries to add a prescription (only doctors can do this).
 */
public class StaffNotAuthorizedException extends Exception {
    private String staffId;
    private String action;
    private String staffType;

    public StaffNotAuthorizedException(String staffId, String action, String staffType) {
        super("Staff member " + staffId + " (type: " + staffType + ") is not authorized to perform action: " + action);
        this.staffId = staffId;
        this.action = action;
        this.staffType = staffType;
    }

    public String getStaffId() { return staffId; }
    public String getAction() { return action; }
    public String getStaffType() { return staffType; }
}
