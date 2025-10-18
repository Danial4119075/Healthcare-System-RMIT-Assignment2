package healthcare.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import healthcare.model.*;
import healthcare.exceptions.*;
import healthcare.utils.ValidationUtils;
import healthcare.utils.AuditLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controls the main dashboard UI after login.
 * This handles everything the user sees - bed layout, patient info, menus, etc.
 */
public class DashboardController {

    // UI components linked from the FXML file
    @FXML private MenuBar menuBar;
    @FXML private Menu staffMenu, patientMenu, shiftMenu, reportMenu;
    @FXML private MenuItem addPrescriptionMenuItem;
    @FXML private GridPane bedDisplayGrid;
    @FXML private TextArea detailsArea;
    @FXML private Label statusLabel;
    @FXML private Label currentStaffLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label currentShiftLabel;
    @FXML private Button refreshButton;

    // Keep track of the main app and who's logged in
    private MainApplication mainApp;
    private String currentStaffId;
    private Staff currentStaff;

    // Timer for the clock that updates every second
    private Timeline clockTimer;

    // Track if we're in "move patient" mode
    private boolean movePatientMode = false;
    private String moveFromPatientId = null;
    private boolean complianceMenuAdded = false;

    @FXML
    private void initialize() {
        setupUI();
    }

    private void setupUI() {
        // Make the details area read-only and wrap long text
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        updateStatus("System ready");
    }

    private void addComplianceMenuItem() {
        // Add the compliance check option to reports menu (only once)
        if (!complianceMenuAdded && reportMenu != null) {
            MenuItem complianceItem = new MenuItem("Check Compliance");
            complianceItem.setOnAction(event -> handleCheckCompliance());
            reportMenu.getItems().add(complianceItem);
            complianceMenuAdded = true;
        }
    }

    public void setMainApplication(MainApplication mainApp) {
        this.mainApp = mainApp;
    }

    public void setCurrentStaff(String staffId) {
        // Set up the dashboard for whoever just logged in
        this.currentStaffId = staffId;
        this.currentStaff = mainApp.getCareHome().getStaff(staffId);
        if (currentStaff != null) {
            currentStaffLabel.setText("Logged in as: " + currentStaff.getName() + " (" + currentStaff.getStaffType() + ")");
            configureMenusForStaffType();
            addComplianceMenuItem();
            refreshBedDisplay();
            startClock();
        }
    }

    private void startClock() {
        // Start the live clock that updates every second
        // Struggled with this initially - needed JavaFX Timeline instead of Thread.sleep()
        updateClock();
        clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
        clockTimer.setCycleCount(Timeline.INDEFINITE);
        clockTimer.play();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy - hh:mm:ss a");

        if (currentTimeLabel != null) {
            currentTimeLabel.setText("⏰ " + now.format(timeFormatter));
        }

        // Show if staff member is currently on shift or not
        if (currentShiftLabel != null && currentStaff != null) {
            boolean onShift = currentStaff.isRosteredNow();

            if (onShift) {
                currentShiftLabel.setText("✓ ON SHIFT");
                currentShiftLabel.setStyle("-fx-text-fill: #00AA00; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                currentShiftLabel.setText("✗ OFF SHIFT");
                currentShiftLabel.setStyle("-fx-text-fill: #CC0000; -fx-font-weight: bold; -fx-font-size: 14px;");
            }

            // Add tooltip showing today's scheduled shifts
            String currentDay = now.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).toUpperCase();
            List<String> todayShifts = currentStaff.getShiftsForDay(currentDay);

            String tooltipText = "Today's shift(s) for " + currentDay + ":\n";
            if (todayShifts != null && !todayShifts.isEmpty()) {
                tooltipText += String.join(", ", todayShifts);
            } else {
                tooltipText += "No shifts assigned";
            }

            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(currentShiftLabel, tooltip);
        }
    }

    public void stopClock() {
        // Clean up the clock when logging out
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

    private void configureMenusForStaffType() {
        // Show/hide menus based on who's logged in (Doctor/Nurse/Manager)
        String staffType = currentStaff.getStaffType();
        reportMenu.setVisible(true);
        staffMenu.setVisible(staffType.equals("Manager"));
        shiftMenu.setVisible(staffType.equals("Manager"));
        patientMenu.setVisible(true);

        // Only doctors can add prescriptions
        if (addPrescriptionMenuItem != null) {
            addPrescriptionMenuItem.setVisible(staffType.equals("Doctor"));
        }

        // Add medication administration option for nurses only
        if (staffType.equals("Nurse")) {
            MenuItem administerMedItem = new MenuItem("Administer Medication");
            administerMedItem.setOnAction(event -> handleAdministerMedication());

            // Make sure we don't add it twice
            boolean alreadyExists = patientMenu.getItems().stream()
                    .anyMatch(item -> item.getText().equals("Administer Medication"));

            if (!alreadyExists) {
                patientMenu.getItems().add(administerMedItem);
            }
        }
    }

    @FXML
    private void handleAdministerMedication() {
        // Nurses can record when they give medication to patients
        if (!currentStaff.canPerformAction("administer_medication")) {
            showError("Only nurses can administer medication");
            return;
        }

        List<Patient> patients = mainApp.getCareHome().getAllPatients();
        if (patients.isEmpty()) {
            showError("No patients in the system");
            return;
        }

        // Create the dialog window for recording medication
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Administer Medication");
        dialog.setHeaderText("Record Medication Administration");

        Label patientLabel = new Label("Select Patient:");
        ComboBox<Patient> patientCombo = new ComboBox<>();
        patientCombo.getItems().addAll(patients);

        // Format how patients appear in the dropdown
        patientCombo.setCellFactory(list -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getName() + " (" + p.getId() + ") - Bed: " + p.getBedId());
            }
        });
        patientCombo.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getName() + " (" + p.getId() + ") - Bed: " + p.getBedId());
            }
        });

        Label medLabel = new Label("Medication Name:");
        TextField medField = new TextField();
        medField.setPromptText("e.g., Amlodipine");

        Label dosageLabel = new Label("Dosage:");
        TextField dosageField = new TextField();
        dosageField.setPromptText("e.g., 5mg");

        Label notesLabel = new Label("Administration Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Enter any notes about the administration...");

        // Show current time for reference
        Label timeLabel = new Label("Administration Time: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");

        VBox content = new VBox(10, patientLabel, patientCombo, medLabel, medField,
                dosageLabel, dosageField, notesLabel, notesArea, timeLabel);
        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);

        ButtonType administerButton = new ButtonType("Administer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(administerButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == administerButton) {
                Patient selectedPatient = patientCombo.getValue();
                if (selectedPatient == null) {
                    showError("Please select a patient");
                    return null;
                }

                String medName = medField.getText().trim();
                String dosage = dosageField.getText().trim();
                String notes = notesArea.getText().trim();

                if (medName.isEmpty() || dosage.isEmpty()) {
                    showError("Medication name and dosage are required");
                    return null;
                }

                try {
                    // Create a unique ID for this medication record
                    String recordId = "MED-" + System.currentTimeMillis();

                    // Build the medication record
                    MedicationRecord record = new MedicationRecord(
                            recordId,
                            selectedPatient.getId(),
                            currentStaffId,
                            medName,
                            dosage
                    );

                    record.setNotes(notes);

                    mainApp.getCareHome().administerMedication(selectedPatient.getId(), record, currentStaffId);
                    showSuccess("Medication administered successfully to " + selectedPatient.getName());

                    // Refresh patient details if they're currently displayed
                    if (detailsArea.getText().contains(selectedPatient.getId())) {
                        showPatientDetails(selectedPatient.getId());
                    }

                    return "success";
                } catch (StaffNotRosteredException e) {
                    showError("You are not currently rostered (on shift).\n\n" + e.getMessage());
                    return null;
                } catch (StaffNotAuthorizedException e) {
                    showError("Authorization error: " + e.getMessage());
                    return null;
                } catch (Exception e) {
                    showError("Failed to administer medication: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleCheckCompliance() {
        // Generate and display the shift compliance report
        try {
            String report = mainApp.getCareHome().generateComplianceReport();

            if (report == null || report.trim().isEmpty()) {
                String message = "No compliance data available.\n\nPlease ensure nurses have assigned shifts in the system.";
                detailsArea.setText(message);
                showInfo(message);
                updateStatus("No compliance data found.");
            } else {
                detailsArea.setText(report);

                // Show it in a popup too for easier reading
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Compliance Report");
                alert.setHeaderText("Staff Shift Compliance Check");

                TextArea textArea = new TextArea(report);
                textArea.setEditable(false);
                textArea.setWrapText(false);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);

                alert.getDialogPane().setContent(textArea);
                alert.getDialogPane().setPrefSize(900, 400);
                alert.showAndWait();

                updateStatus("Compliance report generated.");
            }
        } catch (Exception e) {
            String errorMsg = "Error: " + e.getMessage() + "\n\nStack trace:\n";
            e.printStackTrace();
            detailsArea.setText(errorMsg);
            updateStatus("Failed to generate compliance report.");
            showError("Error generating compliance report: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewAuditLog() {
        // Display all system actions logged in the audit trail
        try {
            AuditLogger auditLogger = AuditLogger.getInstance();
            List<AuditLogger.AuditRecord> records = auditLogger.getAllAuditRecords();

            StringBuilder auditReport = new StringBuilder();
            auditReport.append("AUDIT LOG\n");
            auditReport.append("=========\n\n");
            auditReport.append("Total Records: ").append(records.size()).append("\n\n");

            if (records.isEmpty()) {
                auditReport.append("No audit records found.\n");
            } else {
                for (AuditLogger.AuditRecord record : records) {
                    auditReport.append("[").append(record.getTimestamp()).append("]\n");
                    auditReport.append("Staff ID: ").append(record.getStaffId()).append("\n");
                    auditReport.append("Action: ").append(record.getAction()).append("\n");
                    auditReport.append("Details: ").append(record.getDetails()).append("\n");
                    auditReport.append("---\n");
                }
            }

            detailsArea.setText(auditReport.toString());

            // Also show in a popup window
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Audit Log");
            alert.setHeaderText("System Audit Trail");

            TextArea textArea = new TextArea(auditReport.toString());
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefSize(800, 600);
            alert.showAndWait();

            updateStatus("Audit log displayed");
        } catch (Exception e) {
            showError("Error loading audit log: " + e.getMessage());
        }
    }

    @FXML
    private void refreshBedDisplay() {
        // Rebuild the visual grid showing all beds in all wards
        bedDisplayGrid.getChildren().clear();
        List<Ward> wards = mainApp.getCareHome().getWards();
        int row = 0;
        for (Ward ward : wards) {
            // Add ward name as a header
            Label wardLabel = new Label(ward.getWardName());
            wardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            bedDisplayGrid.add(wardLabel, 0, row, 6, 1);
            row++;

            // Add rooms and beds under each ward
            for (Room room : ward.getRooms()) {
                Label roomLabel = new Label(room.getRoomId());
                roomLabel.setStyle("-fx-font-weight: bold;");
                bedDisplayGrid.add(roomLabel, 0, row);
                int col = 1;
                for (Bed bed : room.getBeds()) {
                    Rectangle bedRect = createBedRectangle(bed);
                    bedDisplayGrid.add(bedRect, col, row);
                    col++;
                }
                row++;
            }
            row++;
        }
        updateStatus("Bed display refreshed");
    }

    private Rectangle createBedRectangle(Bed bed) {
        // Create a colored rectangle to represent each bed
        // Blue = male, pink = female, white = empty
        Rectangle rect = new Rectangle(80, 40);
        if (bed.isOccupied()) {
            Patient patient = mainApp.getCareHome().getPatient(bed.getPatientId());
            if (patient != null) {
                Color color = patient.getGender().equals("M") ? Color.LIGHTBLUE : Color.LIGHTPINK;
                rect.setFill(color);
                Tooltip tooltip = new Tooltip(patient.getName() + " (" + patient.getGender() + ", " + patient.getAge() + ")\nBed: " + bed.getBedId());
                Tooltip.install(rect, tooltip);
            } else {
                rect.setFill(Color.LIGHTGRAY);
            }
        } else {
            rect.setFill(Color.WHITE);
            Tooltip tooltip = new Tooltip("Vacant bed: " + bed.getBedId());
            Tooltip.install(rect, tooltip);
        }
        rect.setStroke(Color.BLACK);

        // Handle clicks on beds (either for moving patients or viewing details)
        rect.setOnMouseClicked(event -> {
            if (movePatientMode) {
                // Two-step process: first click selects patient, second click selects destination
                if (moveFromPatientId == null && bed.isOccupied()) {
                    moveFromPatientId = bed.getPatientId();
                    updateStatus("Selected patient " + moveFromPatientId + ". Now select an empty destination bed.");
                } else if (moveFromPatientId != null && !bed.isOccupied()) {
                    handleMovePatientToBed(bed.getBedId());
                }
            } else {
                handleBedClick(bed);
            }
        });

        return rect;
    }

    private void handleBedClick(Bed bed) {
        // Show patient details or bed options depending on if occupied
        if (bed.isOccupied()) {
            showPatientDetails(bed.getPatientId());
        } else {
            showBedOptions(bed);
        }
    }

    private void showPatientDetails(String patientId) {
        // Display all info about a patient in the details panel
        Patient patient = mainApp.getCareHome().getPatient(patientId);
        if (patient != null) {
            StringBuilder details = new StringBuilder();
            details.append("PATIENT DETAILS\n");
            details.append("================\n");
            details.append("ID: ").append(patient.getId()).append("\n");
            details.append("Name: ").append(patient.getName()).append("\n");
            details.append("Age: ").append(patient.getAge()).append("\n");
            details.append("Gender: ").append(patient.getGender()).append("\n");
            details.append("Bed: ").append(patient.getBedId()).append("\n");
            details.append("Medical Condition: ").append(patient.getMedicalCondition()).append("\n");
            details.append("Requires Isolation: ").append(patient.requiresIsolation() ? "Yes" : "No").append("\n\n");
            details.append("PRESCRIPTIONS:\n");
            List<Prescription> prescriptions = patient.getPrescriptions();
            if (prescriptions.isEmpty()) {
                details.append("No prescriptions\n");
            } else {
                for (Prescription prescription : prescriptions) {
                    details.append("- ").append(prescription.getPrescriptionId())
                            .append(" (Dr. ").append(mainApp.getCareHome().getStaff(prescription.getDoctorId()).getName())
                            .append(")\n");
                    for (Medication med : prescription.getMedications()) {
                        details.append("   * ").append(med.getMedicationName())
                                .append(" - ").append(med.getDosage())
                                .append(" (").append(med.getFrequency()).append(")\n");
                    }
                }
            }
            detailsArea.setText(details.toString());
        }
    }

    private void showBedOptions(Bed bed) {
        detailsArea.setText("Empty bed: " + bed.getBedId() + "\n\n" +
                "Available actions:\n" +
                "- Add new patient (Manager only)\n" +
                "- View bed details");
    }

    @FXML
    private void handleAddPatient() {
        // Open the dialog to admit a new patient
        if (!currentStaff.canPerformAction("add_patient")) {
            showError("You don't have permission to add patients");
            return;
        }
        PatientDialog dialog = new PatientDialog(mainApp.getCareHome().getAvailableBeds());
        Optional<PatientDialog.PatientData> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                PatientDialog.PatientData data = result.get();
                Patient patient = new Patient(
                        data.patientId, data.name, data.email, data.phone,
                        data.dateOfBirth, data.gender, data.medicalCondition, data.requiresIsolation
                );
                mainApp.getCareHome().addPatient(patient, data.bedId, currentStaffId);
                refreshBedDisplay();
                showSuccess("Patient added successfully");
            } catch (Exception e) {
                showError("Failed to add patient: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddPrescription() {
        // Doctors can create prescriptions for patients
        if (!currentStaff.canPerformAction("add_prescription")) {
            showError("Only doctors can add prescriptions");
            return;
        }
        List<Patient> patients = mainApp.getCareHome().getAllPatients();
        if (patients.isEmpty()) {
            showError("No patients in the system");
            return;
        }
        PrescriptionDialog dialog = new PrescriptionDialog(patients);
        Optional<PrescriptionDialog.PrescriptionData> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                PrescriptionDialog.PrescriptionData data = result.get();
                Prescription prescription = new Prescription(
                        data.prescriptionId, data.patientId, currentStaffId, data.notes
                );
                for (Medication med : data.medications) {
                    prescription.addMedication(med);
                }
                mainApp.getCareHome().addPrescription(data.patientId, prescription, currentStaffId);
                showSuccess("Prescription added successfully");
            } catch (Exception e) {
                showError("Failed to add prescription: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleMovePatient() {
        // Enable the two-click move mode (select patient, then destination bed)
        if (!currentStaff.canPerformAction("move_patient")) {
            showError("You don't have permission to move patients");
            return;
        }
        movePatientMode = true;
        moveFromPatientId = null;
        showInfo("Move patient mode: Click the patient (occupied bed), then click a vacant bed to move.");
        updateStatus("Move patient mode enabled. Select patient, then destination bed.");
    }

    private void handleMovePatientToBed(String toBedId) {
        // Actually perform the move operation
        try {
            mainApp.getCareHome().movePatient(moveFromPatientId, toBedId, currentStaffId);
            showSuccess("Patient moved to new bed successfully");
        } catch (Exception e) {
            showError("Failed to move patient: " + e.getMessage());
        }
        refreshBedDisplay();
        movePatientMode = false;
        moveFromPatientId = null;
        updateStatus("Patient move complete");
    }

    /**
     * Discharge a patient and archive their records to the database.
     * Only managers can do this. The patient data gets saved for auditing
     * and the bed becomes available again.
     */
    @FXML
    private void handleDischargePatient() {
        // Only managers can discharge patients
        if (!currentStaff.canPerformAction("discharge_patient")) {
            showError("Only managers can discharge patients");
            return;
        }

        List<Patient> patients = mainApp.getCareHome().getAllPatients();
        if (patients.isEmpty()) {
            showError("No patients in the system to discharge");
            return;
        }

        // Build the discharge dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Discharge Patient");
        dialog.setHeaderText("Select patient to discharge and provide discharge details");

        // Patient dropdown
        Label patientLabel = new Label("Select Patient:");
        ComboBox<Patient> patientCombo = new ComboBox<>();
        patientCombo.getItems().addAll(patients);
        patientCombo.setCellFactory(list -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" :
                        p.getName() + " (" + p.getId() + ") - Bed: " + p.getBedId() +
                                " - Condition: " + p.getMedicalCondition());
            }
        });
        patientCombo.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" :
                        p.getName() + " (" + p.getId() + ") - Bed: " + p.getBedId());
            }
        });

        // Reason for discharge
        Label reasonLabel = new Label("Discharge Reason:");
        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll(
                "Treatment Complete - Recovered",
                "Transfer to Another Facility",
                "Patient Request",
                "Home Care",
                "Deceased",
                "Other"
        );
        reasonCombo.setValue("Treatment Complete - Recovered");

        // Additional notes
        Label notesLabel = new Label("Discharge Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(4);
        notesArea.setPromptText("Enter discharge summary, follow-up instructions, etc.");
        notesArea.setWrapText(true);

        // Warning about what will happen
        Label warningLabel = new Label("⚠️ WARNING: This will archive patient data and free up the bed.");
        warningLabel.setStyle("-fx-text-fill: #CC6600; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox content = new VBox(10,
                patientLabel, patientCombo,
                reasonLabel, reasonCombo,
                notesLabel, notesArea,
                warningLabel
        );
        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);

        ButtonType dischargeButton = new ButtonType("Discharge Patient", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(dischargeButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == dischargeButton) {
                Patient selectedPatient = patientCombo.getValue();
                if (selectedPatient == null) {
                    showError("Please select a patient to discharge");
                    return null;
                }

                String reason = reasonCombo.getValue();
                String notes = notesArea.getText().trim();

                if (reason == null || reason.isEmpty()) {
                    showError("Please select a discharge reason");
                    return null;
                }

                // Double-check with confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Discharge");
                confirmAlert.setHeaderText("Are you sure you want to discharge this patient?");
                confirmAlert.setContentText(
                        "Patient: " + selectedPatient.getName() + " (ID: " + selectedPatient.getId() + ")\n" +
                                "Bed: " + selectedPatient.getBedId() + "\n" +
                                "Reason: " + reason + "\n\n" +
                                "This action will:\n" +
                                "• Archive all patient data to database\n" +
                                "• Free up the bed for new patients\n" +
                                "• Remove patient from active system"
                );

                Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    try {
                        // Actually discharge the patient
                        mainApp.getCareHome().dischargePatient(
                                selectedPatient.getId(),
                                reason,
                                notes,
                                currentStaffId
                        );

                        // Update the UI
                        refreshBedDisplay();
                        detailsArea.clear();

                        showSuccess(
                                "Patient " + selectedPatient.getName() + " discharged successfully!\n\n" +
                                        "✓ Patient data archived to database\n" +
                                        "✓ Bed " + selectedPatient.getBedId() + " is now available\n" +
                                        "✓ Discharge logged to audit trail"
                        );

                        return "discharged";
                    } catch (StaffNotAuthorizedException e) {
                        showError("Authorization error: " + e.getMessage());
                        return null;
                    } catch (IllegalArgumentException e) {
                        showError("Error: " + e.getMessage());
                        return null;
                    } catch (Exception e) {
                        showError("Failed to discharge patient: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleAddStaff() {
        // Managers can add new staff members to the system
        if (!currentStaff.canPerformAction("add_staff")) {
            showError("Only managers can add staff");
            return;
        }
        StaffDialog dialog = new StaffDialog();
        Optional<StaffDialog.StaffData> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                StaffDialog.StaffData data = result.get();
                Staff newStaff;
                // Create the right type of staff based on selection
                switch (data.staffType) {
                    case "Doctor":
                        newStaff = new Doctor(data.staffId, data.name, data.email, data.phone,
                                data.username, data.password, data.specialization);
                        break;
                    case "Nurse":
                        newStaff = new Nurse(data.staffId, data.name, data.email, data.phone,
                                data.username, data.password, data.certification);
                        break;
                    default:
                        newStaff = new Manager(data.staffId, data.name, data.email, data.phone,
                                data.username, data.password);
                        break;
                }
                mainApp.getCareHome().addStaff(newStaff, currentStaffId);
                showSuccess("Staff member added successfully");
            } catch (Exception e) {
                showError("Failed to add staff: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewReports() {
        // Show a quick summary of system stats
        StringBuilder report = new StringBuilder();
        report.append("SYSTEM REPORT\n");
        report.append("=============\n\n");
        List<Patient> patients = mainApp.getCareHome().getAllPatients();
        report.append("Total Patients: ").append(patients.size()).append("\n");
        List<Staff> staff = mainApp.getCareHome().getAllStaff();
        report.append("Total Staff: ").append(staff.size()).append("\n");
        int totalBeds = 0;
        int occupiedBeds = 0;
        for (Ward ward : mainApp.getCareHome().getWards()) {
            totalBeds += ward.getTotalBeds();
            occupiedBeds += (ward.getTotalBeds() - ward.getAvailableBeds());
        }
        report.append("Bed Occupancy: ").append(occupiedBeds).append("/").append(totalBeds).append("\n\n");
        report.append("RECENT AUDIT LOG:\n");
        report.append("(Audit log entries would be displayed here)\n");
        detailsArea.setText(report.toString());
    }

    @FXML
    private void handleManageShift() {
        // Managers can assign or change staff shift schedules
        if (!currentStaff.getStaffType().equals("Manager")) {
            showError("Only managers can manage shifts.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Manage Shifts");
        dialog.setHeaderText("Assign or Reschedule Shifts");

        Label staffLabel = new Label("Select Staff:");
        ComboBox<Staff> staffCombo = new ComboBox<>();
        staffCombo.getItems().addAll(mainApp.getCareHome().getAllStaff());

        staffCombo.setCellFactory(list -> new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s.getName() + " (" + s.getStaffType() + ")");
            }
        });
        staffCombo.setButtonCell(new ListCell<Staff>() {
            @Override
            protected void updateItem(Staff s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s.getName() + " (" + s.getStaffType() + ")");
            }
        });

        Label dayLabel = new Label("Select Day:");
        ComboBox<String> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
        dayCombo.setValue("MON");

        Label shiftLabel = new Label("Select Shift:");
        ComboBox<String> shiftCombo = new ComboBox<>();

        Label actionLabel = new Label("Action:");
        ComboBox<String> actionCombo = new ComboBox<>();
        actionCombo.getItems().addAll("Assign New Shift", "Replace Existing Shift");
        actionCombo.setValue("Assign New Shift");

        Label existingShiftLabel = new Label("");
        existingShiftLabel.setStyle("-fx-text-fill: blue; -fx-font-style: italic;");

        // Update available shifts based on staff type
        staffCombo.setOnAction(e -> {
            Staff selected = staffCombo.getValue();
            if (selected != null) {
                shiftCombo.getItems().clear();
                if (selected instanceof Nurse) {
                    shiftCombo.getItems().addAll("8AM-4PM", "2PM-10PM");
                } else if (selected instanceof Doctor) {
                    shiftCombo.getItems().add("1HR");
                }
                shiftCombo.setValue(shiftCombo.getItems().isEmpty() ? null : shiftCombo.getItems().get(0));
                updateExistingShiftInfo(selected, dayCombo.getValue(), existingShiftLabel);
            }
        });

        // Show existing shifts when day changes
        dayCombo.setOnAction(e -> {
            Staff selected = staffCombo.getValue();
            if (selected != null) {
                updateExistingShiftInfo(selected, dayCombo.getValue(), existingShiftLabel);
            }
        });

        VBox content = new VBox(10, staffLabel, staffCombo, dayLabel, dayCombo,
                existingShiftLabel, actionLabel, actionCombo, shiftLabel, shiftCombo);
        dialog.getDialogPane().setContent(content);

        ButtonType assignButton = new ButtonType("Assign Shift", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignButton, ButtonType.CLOSE);

        dialog.setResultConverter(button -> {
            if (button == assignButton) {
                Staff selected = staffCombo.getValue();
                if (selected == null) {
                    showError("Select a staff member.");
                    return null;
                }
                String day = dayCombo.getValue();
                if (day == null || day.isEmpty()) {
                    showError("Select a day.");
                    return null;
                }
                String shift = shiftCombo.getValue();
                if (shift == null || shift.isEmpty()) {
                    showError("Select a shift.");
                    return null;
                }

                String action = actionCombo.getValue();

                List<String> existingShifts = selected.getShiftsForDay(day);
                boolean hasExistingShift = existingShifts != null && !existingShifts.isEmpty();

                // Make sure they're using the right action for the situation
                if (action.equals("Assign New Shift")) {
                    if (hasExistingShift) {
                        showError(selected.getName() + " already has a shift on " + day + ": " +
                                String.join(", ", existingShifts) +
                                "\n\nPlease select 'Replace Existing Shift' to change it.");
                        return null;
                    }
                } else if (action.equals("Replace Existing Shift")) {
                    if (!hasExistingShift) {
                        showError(selected.getName() + " has no existing shift on " + day +
                                " to replace.\n\nPlease select 'Assign New Shift' instead.");
                        return null;
                    }
                    selected.clearShiftsForDay(day);
                }

                selected.assignShift(day, shift);

                String message = action.equals("Replace Existing Shift") ?
                        "Shift replaced for " : "Shift assigned to ";
                showSuccess(message + selected.getName() + " on " + day + ": " + shift);
                return "assigned";
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void updateExistingShiftInfo(Staff staff, String day, Label label) {
        // Show what shifts are currently assigned for the selected day
        if (staff == null || day == null) {
            label.setText("");
            return;
        }
        List<String> existingShifts = staff.getShiftsForDay(day);
        if (existingShifts != null && !existingShifts.isEmpty()) {
            label.setText("Current shift on " + day + ": " + String.join(", ", existingShifts));
        } else {
            label.setText("No shift currently assigned on " + day);
        }
    }

    @FXML
    private void handleLogout() {
        // Save everything and go back to login screen
        try {
            stopClock();
            mainApp.getCareHome().saveData();
            mainApp.start(mainApp.getPrimaryStage());
        } catch (Exception e) {
            showError("Error during logout: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        // Update the status bar at the bottom
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void showError(String message) {
        mainApp.showErrorDialog("Error", message);
    }

    private void showSuccess(String message) {
        mainApp.showSuccessDialog("Success", message);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
