package healthcare.exceptions;

/**
 * Exception thrown when attempting to assign a patient to an already occupied bed.
 */
public class BedOccupiedException extends Exception {
    private String bedId;
    private String currentPatientId;
    private String attemptedPatientId;

    public BedOccupiedException(String bedId, String currentPatientId, String attemptedPatientId) {
        super("Bed " + bedId + " is already occupied by patient " + currentPatientId + 
              ". Cannot assign patient " + attemptedPatientId);
        this.bedId = bedId;
        this.currentPatientId = currentPatientId;
        this.attemptedPatientId = attemptedPatientId;
    }

    public String getBedId() { return bedId; }
    public String getCurrentPatientId() { return currentPatientId; }
    public String getAttemptedPatientId() { return attemptedPatientId; }
}
