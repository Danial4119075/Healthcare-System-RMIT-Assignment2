package healthcare.exceptions;

import java.time.LocalDateTime;

/**
 * Exception thrown when staff member tries to perform an action outside their rostered hours.
 */
public class StaffNotRosteredException extends Exception {
    private String staffId;
    private LocalDateTime attemptedTime;
    private String staffType;

    public StaffNotRosteredException(String staffId, LocalDateTime attemptedTime, String staffType) {
        super("Staff member " + staffId + " (type: " + staffType + ") is not rostered at time: " + attemptedTime);
        this.staffId = staffId;
        this.attemptedTime = attemptedTime;
        this.staffType = staffType;
    }

    public String getStaffId() { return staffId; }
    public LocalDateTime getAttemptedTime() { return attemptedTime; }
    public String getStaffType() { return staffType; }
}
