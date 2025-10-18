package healthcare.gui;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import healthcare.model.Bed;
import healthcare.utils.ValidationUtils;
import java.time.LocalDate;
import java.util.List;

/**
 * Dialog for adding new patients to the system.
 */
public class PatientDialog extends Dialog<PatientDialog.PatientData> {

    public static class PatientData {
        public String patientId;
        public String name;
        public String email;
        public String phone;
        public LocalDate dateOfBirth;
        public String gender;
        public String medicalCondition;
        public boolean requiresIsolation;
        public String bedId;
    }

    public PatientDialog(List<Bed> availableBeds) {
        setTitle("Add New Patient");
        setHeaderText("Enter patient information");

        // Create form fields
        TextField patientIdField = new TextField();
        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        DatePicker dobPicker = new DatePicker();
        ComboBox<String> genderCombo = new ComboBox<>();
        genderCombo.getItems().addAll("M", "F");
        TextField conditionField = new TextField();
        CheckBox isolationCheck = new CheckBox();
        ComboBox<String> bedCombo = new ComboBox<>();

        // Populate bed combo
        for (Bed bed : availableBeds) {
            bedCombo.getItems().add(bed.getBedId());
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Patient ID:"), 0, 0);
        grid.add(patientIdField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Date of Birth:"), 0, 4);
        grid.add(dobPicker, 1, 4);
        grid.add(new Label("Gender:"), 0, 5);
        grid.add(genderCombo, 1, 5);
        grid.add(new Label("Medical Condition:"), 0, 6);
        grid.add(conditionField, 1, 6);
        grid.add(new Label("Requires Isolation:"), 0, 7);
        grid.add(isolationCheck, 1, 7);
        grid.add(new Label("Bed:"), 0, 8);
        grid.add(bedCombo, 1, 8);

        getDialogPane().setContent(grid);

        // Add buttons
        ButtonType addButton = new ButtonType("Add Patient", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                // Validate input
                if (!validateInput(patientIdField, nameField, emailField, phoneField, 
                                 dobPicker, genderCombo, conditionField, bedCombo)) {
                    return null;
                }

                PatientData data = new PatientData();
                data.patientId = patientIdField.getText().trim();
                data.name = nameField.getText().trim();
                data.email = emailField.getText().trim();
                data.phone = phoneField.getText().trim();
                data.dateOfBirth = dobPicker.getValue();
                data.gender = genderCombo.getValue();
                data.medicalCondition = conditionField.getText().trim();
                data.requiresIsolation = isolationCheck.isSelected();
                data.bedId = bedCombo.getValue();
                return data;
            }
            return null;
        });
    }

    private boolean validateInput(TextField patientIdField, TextField nameField, 
                                TextField emailField, TextField phoneField,
                                DatePicker dobPicker, ComboBox<String> genderCombo,
                                TextField conditionField, ComboBox<String> bedCombo) {

        // Validate all fields
        if (!ValidationUtils.isValidId(patientIdField.getText())) {
            showValidationError("Invalid Patient ID format");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(nameField.getText())) {
            showValidationError("Name is required");
            return false;
        }

        if (!ValidationUtils.isValidEmail(emailField.getText())) {
            showValidationError("Invalid email format");
            return false;
        }

        if (!ValidationUtils.isValidPhone(phoneField.getText())) {
            showValidationError("Invalid phone number format");
            return false;
        }

        if (dobPicker.getValue() == null) {
            showValidationError("Date of birth is required");
            return false;
        }

        if (genderCombo.getValue() == null) {
            showValidationError("Gender selection is required");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(conditionField.getText())) {
            showValidationError("Medical condition is required");
            return false;
        }

        if (bedCombo.getValue() == null) {
            showValidationError("Bed selection is required");
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
