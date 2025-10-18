package healthcare.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Room class represents individual rooms within wards.
 * Each room contains multiple beds (1-4 beds per room).
 */
public class Room implements Serializable {
    private String roomId;
    private String wardId;
    private int bedCount;
    private List<Bed> beds;

    public Room(String roomId, String wardId, int bedCount) {
        this.roomId = roomId;
        this.wardId = wardId;
        this.bedCount = bedCount;
        this.beds = new ArrayList<>();

        // Create beds for this room
        for (int i = 1; i <= bedCount; i++) {
            String bedId = roomId + "-B" + i;
            beds.add(new Bed(bedId, roomId, wardId));
        }
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public int getBedCount() { return bedCount; }

    public List<Bed> getBeds() { return new ArrayList<>(beds); }

    public Bed getBed(String bedId) {
        return beds.stream()
                   .filter(bed -> bed.getBedId().equals(bedId))
                   .findFirst()
                   .orElse(null);
    }

    public int getAvailableBeds() {
        return (int) beds.stream().filter(bed -> !bed.isOccupied()).count();
    }

    public int getOccupiedBeds() {
        return (int) beds.stream().filter(bed -> bed.isOccupied()).count();
    }

    @Override
    public String toString() {
        return "Room{id='" + roomId + "', ward='" + wardId + "', bedCount=" + bedCount + 
               ", available=" + getAvailableBeds() + "/" + bedCount + "}";
    }
}
