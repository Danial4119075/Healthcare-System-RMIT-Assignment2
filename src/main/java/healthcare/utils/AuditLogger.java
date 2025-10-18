package healthcare.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogger utility class for logging all system activities.
 * Implements Singleton pattern to ensure single instance.
 */
public class AuditLogger {
    private static AuditLogger instance;
    private List<AuditRecord> auditRecords;
    private static final String AUDIT_DIR = "data";
    private static final String AUDIT_FILE = AUDIT_DIR + "/audit_log.txt";

    private AuditLogger() {
        this.auditRecords = new ArrayList<>();
        // Ensure 'data' directory exists on instantiation
        File dir = new File(AUDIT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Singleton pattern implementation
    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    /**
     * Log an action performed by a staff member
     */
    public void logAction(String staffId, String action, String details) {
        AuditRecord record = new AuditRecord(staffId, action, details, LocalDateTime.now());
        auditRecords.add(record);
        writeToFile(record);
    }

    /**
     * Get all audit records
     */
    public List<AuditRecord> getAllAuditRecords() {
        return new ArrayList<>(auditRecords);
    }

    /**
     * Get audit records for a specific staff member
     */
    public List<AuditRecord> getAuditRecordsForStaff(String staffId) {
        List<AuditRecord> result = new ArrayList<>();
        for (AuditRecord record : auditRecords) {
            if (record.getStaffId().equals(staffId)) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * Write audit record to file.
     * Creates the 'data' directory and log file if missing.
     */
    private void writeToFile(AuditRecord record) {
        try {
            // Ensure directory exists just prior to writing
            File dir = new File(AUDIT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(AUDIT_FILE, true))) {
                writer.println(record.toString());
            }
        } catch (IOException e) {
            System.err.println("Error writing to audit log: " + e.getMessage());
        }
    }

    /**
     * Inner class representing an audit record
     */
    public static class AuditRecord {
        private String staffId;
        private String action;
        private String details;
        private LocalDateTime timestamp;

        public AuditRecord(String staffId, String action, String details, LocalDateTime timestamp) {
            this.staffId = staffId;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
        }

        // Getters
        public String getStaffId() { return staffId; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return timestamp.format(formatter) + " | Staff: " + staffId + " | Action: " + action + " | Details: " + details;
        }
    }
}
