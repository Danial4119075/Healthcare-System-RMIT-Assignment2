package healthcare.gui;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import healthcare.utils.ValidationUtils;

/**
 * Dialog for adding new staff members.
 */
public class StaffDialog extends Dialog<StaffDialog.StaffData> {

    public static class StaffData {
        public String staffId;
        public String name;
        public String email;
        public String phone;
        public String username;
        public String password;
        public String staffType;
        public String specialization; // For doctors
        public String certification;  // For nurses
    }

    public StaffDialog() {
        setTitle("Add New Staff");
        setHeaderText("Enter staff member information");

        // Create form fields
        TextField staffIdField = new TextField();
        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Doctor", "Nurse", "Manager");
        TextField specializationField = new TextField();
        TextField certificationField = new TextField();

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Staff ID:"), 0, 0);
        grid.add(staffIdField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Username:"), 0, 4);
        grid.add(usernameField, 1, 4);
        grid.add(new Label("Password:"), 0, 5);
        grid.add(passwordField, 1, 5);
        grid.add(new Label("Staff Type:"), 0, 6);
        grid.add(typeCombo, 1, 6);
        grid.add(new Label("Specialization (Doctor):"), 0, 7);
        grid.add(specializationField, 1, 7);
        grid.add(new Label("Certification (Nurse):"), 0, 8);
        grid.add(certificationField, 1, 8);

        // Show/hide fields based on staff type
        typeCombo.setOnAction(e -> {
            String type = typeCombo.getValue();
            specializationField.setDisable(!"Doctor".equals(type));
            certificationField.setDisable(!"Nurse".equals(type));
        });

        getDialogPane().setContent(grid);

        // Add buttons
        ButtonType addButton = new ButtonType("Add Staff", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                if (!validateInput(staffIdField, nameField, emailField, phoneField, 
                                 usernameField, passwordField, typeCombo)) {
                    return null;
                }

                StaffData data = new StaffData();
                data.staffId = staffIdField.getText().trim();
                data.name = nameField.getText().trim();
                data.email = emailField.getText().trim();
                data.phone = phoneField.getText().trim();
                data.username = usernameField.getText().trim();
                data.password = passwordField.getText();
                data.staffType = typeCombo.getValue();
                data.specialization = specializationField.getText().trim();
                data.certification = certificationField.getText().trim();
                return data;
            }
            return null;
        });
    }

    private boolean validateInput(TextField staffIdField, TextField nameField,
                                TextField emailField, TextField phoneField,
                                TextField usernameField, PasswordField passwordField,
                                ComboBox<String> typeCombo) {

        if (!ValidationUtils.isValidId(staffIdField.getText())) {
            showValidationError("Invalid Staff ID format");
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

        if (!ValidationUtils.isNotEmpty(usernameField.getText())) {
            showValidationError("Username is required");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(passwordField.getText())) {
            showValidationError("Password is required");
            return false;
        }

        if (typeCombo.getValue() == null) {
            showValidationError("Staff type selection is required");
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
