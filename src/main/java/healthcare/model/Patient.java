package healthcare.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient class that extends Person.
 * Represents residents in the healthcare facility.
 */
public class Patient extends Person implements Serializable {
    private LocalDate dateOfBirth;
    private String gender; // "M" or "F"
    private String medicalCondition;
    private boolean requiresIsolation;
    private String bedId; // Current bed assignment
    private List<Prescription> prescriptions;
    private List<MedicationRecord> medicationHistory;

    public Patient(String id, String name, String email, String phone, LocalDate dateOfBirth, String gender, String medicalCondition, boolean requiresIsolation) {
        super(id, name, email, phone);
        this.dateOfBirth = dateOfBirth;
        this.gender = gender.toUpperCase();
        this.medicalCondition = medicalCondition;
        this.requiresIsolation = requiresIsolation;
        this.prescriptions = new ArrayList<>();
        this.medicationHistory = new ArrayList<>();
    }

    // Getters and Setters
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender.toUpperCase(); }

    public String getMedicalCondition() { return medicalCondition; }
    public void setMedicalCondition(String medicalCondition) { this.medicalCondition = medicalCondition; }

    public boolean requiresIsolation() { return requiresIsolation; }
    public void setRequiresIsolation(boolean requiresIsolation) { this.requiresIsolation = requiresIsolation; }

    public String getBedId() { return bedId; }
    public void setBedId(String bedId) { this.bedId = bedId; }

    public List<Prescription> getPrescriptions() { return new ArrayList<>(prescriptions); }
    public void addPrescription(Prescription prescription) { this.prescriptions.add(prescription); }

    public List<MedicationRecord> getMedicationHistory() { return new ArrayList<>(medicationHistory); }
    public void addMedicationRecord(MedicationRecord record) { this.medicationHistory.add(record); }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "Patient{id='" + id + "', name='" + name + "', gender='" + gender + "', age=" + getAge() + 
               ", bedId='" + bedId + "', condition='" + medicalCondition + "'}";
    }
}
