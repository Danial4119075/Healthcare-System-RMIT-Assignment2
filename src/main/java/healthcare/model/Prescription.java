package healthcare.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Prescription class represents a medical prescription for a patient.
 * Contains multiple medications with dosage and timing information.
 */
public class Prescription implements Serializable {
    private String prescriptionId;
    private String patientId;
    private String doctorId;
    private LocalDateTime prescriptionDate;
    private List<Medication> medications;
    private String notes;

    public Prescription(String prescriptionId, String patientId, String doctorId, String notes) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.prescriptionDate = LocalDateTime.now();
        this.medications = new ArrayList<>();
        this.notes = notes;
    }

    // Getters and Setters
    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getPrescriptionDate() { return prescriptionDate; }
    public void setPrescriptionDate(LocalDateTime prescriptionDate) { this.prescriptionDate = prescriptionDate; }

    public List<Medication> getMedications() { return new ArrayList<>(medications); }
    public void addMedication(Medication medication) { this.medications.add(medication); }
    public void removeMedication(Medication medication) { this.medications.remove(medication); }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Prescription{id='" + prescriptionId + "', patientId='" + patientId + 
               "', doctorId='" + doctorId + "', date=" + prescriptionDate + 
               ", medications=" + medications.size() + "}";
    }
}
