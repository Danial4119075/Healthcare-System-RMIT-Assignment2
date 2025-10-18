package healthcare.model;

import java.io.Serializable;

/**
 * Bed class represents individual beds in the healthcare facility.
 * Each bed can be occupied by at most one patient.
 */
public class Bed implements Serializable {
    private String bedId;
    private String roomId;
    private String wardId;
    private boolean isOccupied;
    private String patientId; // null if bed is vacant

    public Bed(String bedId, String roomId, String wardId) {
        this.bedId = bedId;
        this.roomId = roomId;
        this.wardId = wardId;
        this.isOccupied = false;
        this.patientId = null;
    }

    // Getters and Setters
    public String getBedId() { return bedId; }
    public void setBedId(String bedId) { this.bedId = bedId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { this.isOccupied = occupied; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { 
        this.patientId = patientId; 
        this.isOccupied = (patientId != null);
    }

    public void assignPatient(String patientId) {
        this.patientId = patientId;
        this.isOccupied = true;
    }

    public void vacateBed() {
        this.patientId = null;
        this.isOccupied = false;
    }

    @Override
    public String toString() {
        return "Bed{id='" + bedId + "', room='" + roomId + "', ward='" + wardId + 
               "', occupied=" + isOccupied + ", patientId='" + patientId + "'}";
    }
}
