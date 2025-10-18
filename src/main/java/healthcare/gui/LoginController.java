package healthcare.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import healthcare.model.Staff;
import healthcare.utils.ValidationUtils;

/**
 * Controller for the login screen.
 * Handles staff authentication and navigation to main dashboard.
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private MainApplication mainApp;

    /**
     * Initialize the controller
     */
    @FXML
    private void initialize() {
        // Clear error label initially
        errorLabel.setText("");

        // Set focus on username field
        usernameField.requestFocus();

        // Add enter key handling
        passwordField.setOnAction(event -> handleLogin());
    }

    /**
     * Set reference to main application
     */
    public void setMainApplication(MainApplication mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Clear previous error
        errorLabel.setText("");

        // Validate input
        if (!ValidationUtils.isNotEmpty(username)) {
            showError("Please enter a username");
            return;
        }

        if (!ValidationUtils.isNotEmpty(password)) {
            showError("Please enter a password");
            return;
        }

        try {
            // Authenticate user
            Staff staff = mainApp.getCareHome().authenticateStaff(username, password);

            if (staff != null) {
                // Login successful - go to dashboard
                mainApp.showMainDashboard(staff.getId());
            } else {
                showError("Invalid username or password");
            }

        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    /**
     * Handle demo login - for testing purposes
     */
    @FXML
    private void handleDemoLogin() {
        usernameField.setText("admin");
        passwordField.setText("admin123");
        handleLogin();
    }
}
