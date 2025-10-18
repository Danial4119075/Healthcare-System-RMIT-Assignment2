package healthcare.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ward class represents hospital wards containing multiple rooms.
 */
public class Ward implements Serializable {
    private String wardId;
    private String wardName;
    private List<Room> rooms;

    public Ward(String wardId, String wardName) {
        this.wardId = wardId;
        this.wardName = wardName;
        this.rooms = new ArrayList<>();
    }

    // Getters and Setters
    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public List<Room> getRooms() { return new ArrayList<>(rooms); }
    public void addRoom(Room room) { this.rooms.add(room); }

    public Room getRoom(String roomId) {
        return rooms.stream()
                    .filter(room -> room.getRoomId().equals(roomId))
                    .findFirst()
                    .orElse(null);
    }

    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Room room : rooms) {
            allBeds.addAll(room.getBeds());
        }
        return allBeds;
    }

    public int getTotalBeds() {
        return rooms.stream().mapToInt(Room::getBedCount).sum();
    }

    public int getAvailableBeds() {
        return rooms.stream().mapToInt(Room::getAvailableBeds).sum();
    }

    @Override
    public String toString() {
        return "Ward{id='" + wardId + "', name='" + wardName + "', rooms=" + rooms.size() + 
               ", available=" + getAvailableBeds() + "/" + getTotalBeds() + "}";
    }
}
