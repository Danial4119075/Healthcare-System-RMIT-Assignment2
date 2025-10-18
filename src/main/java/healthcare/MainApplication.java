package healthcare.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import healthcare.model.CareHome;

/**
 * Main JavaFX Application class.
 * Entry point for the Healthcare System GUI application.
 */
public class MainApplication extends Application {
    private CareHome careHome;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            // Initialize or load care home data
            initializeCareHome();

            // Load the login screen
            showLoginScreen();

        } catch (Exception e) {
            showErrorDialog("Startup Error", "Failed to initialize application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize the care home system
     */
    private void initializeCareHome() {
        try {
            // Try to load existing data
            careHome = CareHome.loadData();

            // If no existing data, create sample data
            if (careHome.getAllStaff().isEmpty()) {
                careHome.createSampleData();
            }

        } catch (Exception e) {
            // If loading fails, create new instance with sample data
            careHome = CareHome.getInstance();
            careHome.createSampleData();
        }
    }

    /**
     * Show login screen
     */
    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);

            LoginController controller = loader.getController();
            controller.setMainApplication(this);

            primaryStage.setTitle("Healthcare System - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            showErrorDialog("UI Error", "Failed to load login screen: " + e.getMessage());
        }
    }

    /**
     * Show main dashboard after successful login
     */
    public void showMainDashboard(String staffId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            DashboardController controller = loader.getController();
            controller.setMainApplication(this);
            controller.setCurrentStaff(staffId);

            primaryStage.setTitle("Healthcare System - Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();

        } catch (Exception e) {
            showErrorDialog("UI Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    /**
     * Get the care home instance
     */
    public CareHome getCareHome() {
        return careHome;
    }

    /**
     * Get the primary stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Show error dialog
     */
    public void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show success dialog
     */
    public void showSuccessDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Application shutdown - save data
     */
    @Override
    public void stop() {
        try {
            if (careHome != null) {
                careHome.saveData();
            }
        } catch (Exception e) {
            System.err.println("Error saving data on shutdown: " + e.getMessage());
        }
    }

    /**
     * Main method - launch the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}
