package healthcare.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Abstract Staff class that extends Person.
 * Represents all staff members in the healthcare system.
 * Implements Serializable for data persistence.
 */
public abstract class Staff extends Person implements Serializable {
    protected String username;
    protected String password;
    protected String staffType;

    // Map of day ("MON", "TUE", ...) to list of shift time slots (e.g. "8AM-4PM")
    protected Map<String, List<String>> weeklyShifts;

    public Staff(String id, String name, String email, String phone, String username, String password, String staffType) {
        super(id, name, email, phone);
        this.username = username;
        this.password = password;
        this.staffType = staffType;
        this.weeklyShifts = new HashMap<>();
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStaffType() { return staffType; }
    public void setStaffType(String staffType) { this.staffType = staffType; }

    // Assign a shift for a specific day (e.g. "MON", "TUE") and time slot (e.g. "8AM-4PM")
    public void assignShift(String day, String timeSlot) {
        List<String> shifts = weeklyShifts.getOrDefault(day, new ArrayList<>());

        if (this instanceof Nurse) {
            // Only allow "8AM-4PM" or "2PM-10PM"
            if (!timeSlot.equals("8AM-4PM") && !timeSlot.equals("2PM-10PM")) {
                throw new IllegalArgumentException("Nurse can only have '8AM-4PM' or '2PM-10PM' shifts.");
            }
            if (shifts.size() >= 1) {
                throw new IllegalArgumentException("Nurse can only have one shift per day (8 hours).");
            }
        } else if (this instanceof Doctor) {
            // Only one unique shift per day for doctor
            if (shifts.size() >= 1) {
                throw new IllegalArgumentException("Doctor can only have one shift per day.");
            }
            if (shifts.contains(timeSlot)) {
                throw new IllegalArgumentException("Doctor already has this shift for this day.");
            }
        }
        shifts.add(timeSlot);
        weeklyShifts.put(day, shifts);
    }

    // --- ADD SHIFT METHOD FOR CONTROLLER ---
    public void addShift(String shift) {
        // Default to "MON" if no day provided (can be enhanced later)
        String day = "MON";
        assignShift(day, shift);
    }

    // Remove a shift for a specific day and time slot
    public void removeShift(String day, String timeSlot) {
        if (weeklyShifts.containsKey(day)) {
            weeklyShifts.get(day).remove(timeSlot);
        }
    }

    // Clear all shifts for a specific day (NEW METHOD)
    public void clearShiftsForDay(String day) {
        if (weeklyShifts.containsKey(day)) {
            weeklyShifts.get(day).clear();
        }
    }

    // Get all shifts for a specific day
    public List<String> getShiftsForDay(String day) {
        return weeklyShifts.getOrDefault(day, new ArrayList<>());
    }

    // Get total number of shifts assigned in the week
    public int getTotalWeeklyShifts() {
        int total = 0;
        for (List<String> shifts : weeklyShifts.values()) {
            total += shifts.size();
        }
        return total;
    }

    // Get total hours assigned for a specific day (assuming 8-hour shifts for nurses)
    public int getDailyHours(String day) {
        return getShiftsForDay(day).size() * 8;
    }

    // Get total hours assigned for the week (for doctors, 1 hour per shift)
    public int getTotalWeeklyHours(int hoursPerShift) {
        int total = 0;
        for (List<String> shifts : weeklyShifts.values()) {
            total += shifts.size() * hoursPerShift;
        }
        return total;
    }

    // Get all assigned shifts as a map
    public Map<String, List<String>> getWeeklyShifts() {
        // Return a deep copy to prevent external modification
        Map<String, List<String>> copy = new HashMap<>();
        for (String day : weeklyShifts.keySet()) {
            copy.put(day, new ArrayList<>(weeklyShifts.get(day)));
        }
        return copy;
    }

    public void setWeeklyShifts(Map<String, List<String>> shifts) {
        this.weeklyShifts = new HashMap<>();
        for (String day : shifts.keySet()) {
            this.weeklyShifts.put(day, new ArrayList<>(shifts.get(day)));
        }
    }

    // Abstract method for role-specific functionality
    public abstract boolean canPerformAction(String action);

    /**
     * Check if a staff member is rostered (scheduled) right now.
     * For nurses: Only "8AM-4PM" or "2PM-10PM" shifts are valid.
     * For doctors: Any day with any shift (e.g. "1HR") means they are rostered any time that day.
     * Adjust as needed for your specific shift rules!
     */
    public boolean isRosteredNow() {
        LocalDateTime now = LocalDateTime.now();
        // Returns short day name like "MON", "TUE"...
        String currentDay = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        int hour = now.getHour(); // 0-23

        List<String> shifts = getShiftsForDay(currentDay);

        for (String shift : shifts) {
            // Nurse shift logic
            if (shift.equals("8AM-4PM") && hour >= 8 && hour < 16) return true;
            if (shift.equals("2PM-10PM") && hour >= 14 && hour < 22) return true;
            // Doctor shift logic (default: any "1HR" means can act any time that day)
            if (shift.equals("1HR")) return true;
            // If you add other shift patterns, expand here!
        }
        return false; // Not on shift at this time
    }

    @Override
    public String toString() {
        return staffType + "{id='" + id + "', name='" + name + "', username='" + username + "'}";
    }
}
