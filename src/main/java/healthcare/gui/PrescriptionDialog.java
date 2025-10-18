package healthcare.gui;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import healthcare.model.Patient;
import healthcare.model.Medication;
import healthcare.utils.ValidationUtils;
import java.util.List;
import java.util.ArrayList;

/**
 * Dialog for adding prescriptions.
 */
public class PrescriptionDialog extends Dialog<PrescriptionDialog.PrescriptionData> {

    public static class PrescriptionData {
        public String prescriptionId;
        public String patientId;
        public String notes;
        public List<Medication> medications;
    }

    private List<Medication> medications = new ArrayList<>();
    private ListView<Medication> medicationList = new ListView<>();

    public PrescriptionDialog(List<Patient> patients) {
        setTitle("Add Prescription");
        setHeaderText("Create new prescription");

        // Create form fields
        TextField prescriptionIdField = new TextField();
        ComboBox<Patient> patientCombo = new ComboBox<>();
        patientCombo.getItems().addAll(patients);
        patientCombo.setCellFactory(listView -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                setText(empty ? "" : patient.getName() + " (" + patient.getId() + ")");
            }
        });
        patientCombo.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                setText(empty ? "" : patient.getName() + " (" + patient.getId() + ")");
            }
        });

        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);

        // Medication form
        TextField medNameField = new TextField();
        TextField dosageField = new TextField();
        TextField frequencyField = new TextField();
        TextField timesField = new TextField();
        TextField instructionsField = new TextField();
        Button addMedButton = new Button("Add Medication");

        addMedButton.setOnAction(e -> {
            if (validateMedicationInput(medNameField, dosageField, frequencyField, timesField)) {
                Medication med = new Medication(
                    medNameField.getText().trim(),
                    dosageField.getText().trim(),
                    frequencyField.getText().trim(),
                    timesField.getText().trim(),
                    instructionsField.getText().trim()
                );
                medications.add(med);
                medicationList.getItems().add(med);

                // Clear fields
                medNameField.clear();
                dosageField.clear();
                frequencyField.clear();
                timesField.clear();
                instructionsField.clear();
            }
        });

        // Layout
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        GridPane prescriptionGrid = new GridPane();
        prescriptionGrid.setHgap(10);
        prescriptionGrid.setVgap(10);

        prescriptionGrid.add(new Label("Prescription ID:"), 0, 0);
        prescriptionGrid.add(prescriptionIdField, 1, 0);
        prescriptionGrid.add(new Label("Patient:"), 0, 1);
        prescriptionGrid.add(patientCombo, 1, 1);
        prescriptionGrid.add(new Label("Notes:"), 0, 2);
        prescriptionGrid.add(notesArea, 1, 2);

        // Medication section
        Label medLabel = new Label("Medications:");
        medLabel.setStyle("-fx-font-weight: bold;");

        GridPane medGrid = new GridPane();
        medGrid.setHgap(10);
        medGrid.setVgap(5);

        medGrid.add(new Label("Medication:"), 0, 0);
        medGrid.add(medNameField, 1, 0);
        medGrid.add(new Label("Dosage:"), 0, 1);
        medGrid.add(dosageField, 1, 1);
        medGrid.add(new Label("Frequency:"), 0, 2);
        medGrid.add(frequencyField, 1, 2);
        medGrid.add(new Label("Times:"), 0, 3);
        medGrid.add(timesField, 1, 3);
        medGrid.add(new Label("Instructions:"), 0, 4);
        medGrid.add(instructionsField, 1, 4);
        medGrid.add(addMedButton, 1, 5);

        medicationList.setPrefHeight(100);
        medicationList.setCellFactory(listView -> new ListCell<Medication>() {
            @Override
            protected void updateItem(Medication medication, boolean empty) {
                super.updateItem(medication, empty);
                setText(empty ? "" : medication.getMedicationName() + " - " + medication.getDosage());
            }
        });

        mainLayout.getChildren().addAll(prescriptionGrid, medLabel, medGrid, 
                                       new Label("Added Medications:"), medicationList);

        getDialogPane().setContent(mainLayout);

        // Add buttons
        ButtonType addButton = new ButtonType("Add Prescription", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                if (!validatePrescriptionInput(prescriptionIdField, patientCombo)) {
                    return null;
                }

                if (medications.isEmpty()) {
                    showValidationError("At least one medication must be added");
                    return null;
                }

                PrescriptionData data = new PrescriptionData();
                data.prescriptionId = prescriptionIdField.getText().trim();
                data.patientId = patientCombo.getValue().getId();
                data.notes = notesArea.getText().trim();
                data.medications = new ArrayList<>(medications);
                return data;
            }
            return null;
        });
    }

    private boolean validatePrescriptionInput(TextField prescriptionIdField, ComboBox<Patient> patientCombo) {
        if (!ValidationUtils.isNotEmpty(prescriptionIdField.getText())) {
            showValidationError("Prescription ID is required");
            return false;
        }

        if (patientCombo.getValue() == null) {
            showValidationError("Patient selection is required");
            return false;
        }

        return true;
    }

    private boolean validateMedicationInput(TextField medNameField, TextField dosageField, 
                                          TextField frequencyField, TextField timesField) {
        if (!ValidationUtils.isNotEmpty(medNameField.getText())) {
            showValidationError("Medication name is required");
            return false;
        }

        if (!ValidationUtils.isValidDosage(dosageField.getText())) {
            showValidationError("Invalid dosage format (e.g., '250mg', '2 tablets')");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(frequencyField.getText())) {
            showValidationError("Frequency is required");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(timesField.getText())) {
            showValidationError("Administration times are required");
            return false;
        }

        return true;
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
