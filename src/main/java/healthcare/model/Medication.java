package healthcare.model;

import java.io.Serializable;

/**
 * Medication class represents individual medication details within a prescription.
 */
public class Medication implements Serializable {
    private String medicationName;
    private String dosage;
    private String frequency; // e.g., "twice daily", "every 8 hours"
    private String administrationTime; // e.g., "8:00, 16:00"
    private String instructions;

    public Medication(String medicationName, String dosage, String frequency, String administrationTime, String instructions) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.administrationTime = administrationTime;
        this.instructions = instructions;
    }

    // Getters and Setters
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getAdministrationTime() { return administrationTime; }
    public void setAdministrationTime(String administrationTime) { this.administrationTime = administrationTime; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    @Override
    public String toString() {
        return "Medication{name='" + medicationName + "', dosage='" + dosage + 
               "', frequency='" + frequency + "', times='" + administrationTime + "'}";
    }
}
