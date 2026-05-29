package com.elite.erp;

import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.network.NotificationServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;


/**
 * MainApp — Primary JavaFX Application entry point.
 * Handles Stage lifecycle, scene switching, and startup of background services.
 *
 * Architecture: Presentation Layer entry point (Layered Architecture).
 */
public class MainApp extends Application {

    // ── Singleton stage reference for scene switching ────────────────────────
    private static Stage primaryStage;

    // ── Shared wizard data passed between all 5 admission steps ─────────────
    private static AdmissionApplication currentApplication = new AdmissionApplication();

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // 1. Initialise database schema (JDBC layer)
        DatabaseManager.initializeSchema();

        // 2. Start notification server in daemon thread (Socket Programming)
        NotificationServer.start();
        com.elite.erp.network.ChatServer.start();
        com.elite.erp.network.LibraryServer.start();

        // 3. Configure stage
        stage.setTitle("Elite International School — ERP System");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setOnCloseRequest(e -> {
            NotificationServer.stop();
            com.elite.erp.network.ChatServer.stop();
            com.elite.erp.network.LibraryServer.stop();
            DatabaseManager.closeConnection();
            Platform.exit();
        });

        // 4. Load login scene
        switchScene("/com/elite/erp/fxml/Login.fxml");
        stage.show();
    }

    // ── Scene Switching Utility ──────────────────────────────────────────────

    /**
     * Switch the primary stage to any FXML scene.
     * Catches ALL exceptions so failures are never silently swallowed.
     */
    public static void switchScene(String fxmlPath) {
        try {
            URL resource = MainApp.class.getResource(fxmlPath);
            if (resource == null) {
                showError("FXML Not Found", "Resource missing: " + fxmlPath
                        + "\nCheck that the file exists in src/main/resources.");
                return;
            }
            Parent root = FXMLLoader.load(resource);
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1100, 720);
                URL cssUrl = MainApp.class.getResource("/com/elite/erp/css/Premium.css");
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.sizeToScene();
            primaryStage.centerOnScreen();
        } catch (Exception e) {          // ← was IOException only; now catches ClassCastException etc.
            e.printStackTrace();
            showError("Navigation Error",
                    "Failed to load: " + fxmlPath + "\n\nCause: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
    }

    private static void showError(String title, String body) {
        System.err.println("[MainApp] " + title + " — " + body);
        try {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(body);
            alert.showAndWait();
        } catch (Exception ignored) {}
    }

    // ── Wizard state accessors ───────────────────────────────────────────────

    public static AdmissionApplication getCurrentApplication() {
        return currentApplication;
    }

    public static void resetApplication() {
        currentApplication = new AdmissionApplication();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    // ── Session ──────────────────────────────────────────────────────────────
    
    private static com.elite.erp.model.User currentUser;
    
    public static com.elite.erp.model.User getCurrentUser() {
        return currentUser;
    }
    
    public static void setCurrentUser(com.elite.erp.model.User user) {
        currentUser = user;
    }
    
    public static void logout() {
        currentUser = null;
        switchScene("/com/elite/erp/fxml/Login.fxml");
    }

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}
