package healthcare.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MedicationRecord class tracks when medications are actually administered to patients.
 * Used for audit trails and compliance tracking.
 */
public class MedicationRecord implements Serializable {
    private String recordId;
    private String patientId;
    private String nurseId;
    private String medicationName;
    private String dosageGiven;
    private LocalDateTime administrationTime;
    private String notes;
    private boolean administered; // true if successfully administered

    public MedicationRecord(String recordId, String patientId, String nurseId, String medicationName, String dosageGiven) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.nurseId = nurseId;
        this.medicationName = medicationName;
        this.dosageGiven = dosageGiven;
        this.administrationTime = LocalDateTime.now();
        this.administered = false;
        this.notes = "";
    }

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosageGiven() { return dosageGiven; }
    public void setDosageGiven(String dosageGiven) { this.dosageGiven = dosageGiven; }

    public LocalDateTime getAdministrationTime() { return administrationTime; }
    public void setAdministrationTime(LocalDateTime administrationTime) { this.administrationTime = administrationTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isAdministered() { return administered; }
    public void setAdministered(boolean administered) { this.administered = administered; }

    @Override
    public String toString() {
        return "MedicationRecord{recordId='" + recordId + "', patientId='" + patientId + 
               "', medication='" + medicationName + "', time=" + administrationTime + 
               ", administered=" + administered + "}";
    }
}
