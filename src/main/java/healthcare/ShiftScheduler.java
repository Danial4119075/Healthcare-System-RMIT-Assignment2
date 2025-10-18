package healthcare.utils;

import healthcare.model.Staff;
import healthcare.model.Nurse;
import healthcare.model.Doctor;
import java.time.LocalTime;
import java.util.*;

/**
 * ShiftScheduler utility class for managing staff shifts and compliance checking.
 * Ensures nurses work appropriate shifts and doctors are available for prescriptions.
 */
public class ShiftScheduler {
    private Map<String, List<String>> nurseShifts; // nurseid -> list of shifts
    private Map<String, List<String>> doctorShifts; // doctorid -> list of shifts

    // Predefined shift patterns
    public static final String MORNING_SHIFT = "8AM-4PM";
    public static final String AFTERNOON_SHIFT = "2PM-10PM";
    public static final String DOCTOR_SHIFT = "Doctor_Hour";

    public ShiftScheduler() {
        this.nurseShifts = new HashMap<>();
        this.doctorShifts = new HashMap<>();
    }

    /**
     * Assign shifts to a nurse (max 8 hours per day, two shifts available)
     */
    public void assignNurseShifts(String nurseId, List<String> shifts) throws IllegalArgumentException {
        // Validate that nurse doesn't exceed 8 hours per day
        for (String shift : shifts) {
            if (!shift.equals(MORNING_SHIFT) && !shift.equals(AFTERNOON_SHIFT)) {
                throw new IllegalArgumentException("Invalid shift: " + shift);
            }
        }

        // Check no more than one shift per day (8 hours max)
        if (shifts.size() > 7) { // 7 days a week, max 1 shift per day
            throw new IllegalArgumentException("Nurse cannot be assigned more than 7 shifts (1 per day)");
        }

        nurseShifts.put(nurseId, new ArrayList<>(shifts));
    }

    /**
     * Assign doctor shifts (1 hour per day, 7 days a week)
     */
    public void assignDoctorShifts(String doctorId, List<String> shifts) {
        doctorShifts.put(doctorId, new ArrayList<>(shifts));
    }

    /**
     * Check if a staff member is currently on shift
     */
    public boolean isStaffOnShift(String staffId, LocalTime currentTime, String staffType) {
        if (staffType.equals("Nurse")) {
            List<String> shifts = nurseShifts.get(staffId);
            if (shifts == null) return false;

            for (String shift : shifts) {
                if (shift.equals(MORNING_SHIFT)) {
                    if (currentTime.isAfter(LocalTime.of(8, 0)) && currentTime.isBefore(LocalTime.of(16, 0))) {
                        return true;
                    }
                } else if (shift.equals(AFTERNOON_SHIFT)) {
                    if (currentTime.isAfter(LocalTime.of(14, 0)) && currentTime.isBefore(LocalTime.of(22, 0))) {
                        return true;
                    }
                }
            }
        } else if (staffType.equals("Doctor")) {
            List<String> shifts = doctorShifts.get(staffId);
            return shifts != null && !shifts.isEmpty(); // Doctors available for 1 hour daily
        }

        return false;
    }

    /**
     * Get shifts for a staff member
     */
    public List<String> getShifts(String staffId, String staffType) {
        if (staffType.equals("Nurse")) {
            return nurseShifts.getOrDefault(staffId, new ArrayList<>());
        } else if (staffType.equals("Doctor")) {
            return doctorShifts.getOrDefault(staffId, new ArrayList<>());
        }
        return new ArrayList<>();
    }

    /**
     * Check compliance with shift regulations
     */
    public void checkCompliance() throws IllegalStateException {
        // Check that all shifts are covered (14 nurse shifts per week, 7 doctor shifts)
        int totalNurseShifts = nurseShifts.values().stream().mapToInt(List::size).sum();
        int totalDoctorShifts = doctorShifts.values().stream().mapToInt(List::size).sum();

        if (totalNurseShifts < 14) {
            throw new IllegalStateException("Not enough nurse shifts scheduled. Required: 14, Actual: " + totalNurseShifts);
        }

        if (totalDoctorShifts < 7) {
            throw new IllegalStateException("Not enough doctor shifts scheduled. Required: 7, Actual: " + totalDoctorShifts);
        }

        // Check no nurse exceeds 8 hours per day
        for (Map.Entry<String, List<String>> entry : nurseShifts.entrySet()) {
            if (entry.getValue().size() > 7) {
                throw new IllegalStateException("Nurse " + entry.getKey() + " assigned too many shifts");
            }
        }
    }
}
