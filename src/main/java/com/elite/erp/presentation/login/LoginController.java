package com.elite.erp.presentation.login;

import com.elite.erp.MainApp;
import com.elite.erp.dao.UserDAO;
import com.elite.erp.model.User;
import com.elite.erp.util.Response;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController — controls the Login.fxml view.
 * Handles credential validation, scene switching, and link actions.
 */
public class LoginController implements Initializable {

    private final UserDAO userDAO = new UserDAO();

    @FXML private VBox    loginCard;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label   errorLabel;
    @FXML private Button  loginButton;
    @FXML private Hyperlink applyLink;
    @FXML private Hyperlink joinLink;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Fade-in animation on card load
        FadeTransition fade = new FadeTransition(Duration.millis(800), loginCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(600), loginCard);
        slide.setFromY(30);
        slide.setToY(0);

        ParallelTransition intro = new ParallelTransition(fade, slide);
        intro.play();

        errorLabel.setVisible(false);

        // Allow Enter key to submit
        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Gate 1: First-time student login (before credentials are set) ──────
        // Student enters their Application ID (numeric) or student_email in the
        // email field. Password field can be empty at this stage.
        if (userDAO.checkInitialLogin(email).isSuccess()) {
            try {
                MainApp.switchScene("/com/elite/erp/fxml/SetPassword.fxml");
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Navigation error: " + ex.getMessage());
            }
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            shakeCard();
            return;
        }

        // ── Gate 2: Normal credential authentication ──────────────────────────
        Response<User> authResp = userDAO.authenticate(email, password);
        if (!authResp.isSuccess()) {
            showError("Invalid credentials. If this is your first login, enter your Application ID above.");
            shakeCard();
            passwordField.clear();
            return;
        }

        User user = authResp.getData();
        MainApp.setCurrentUser(user);

        // ── Gate 3: Post-auth intercept — student may have set password but
        // needs_password_setup was never cleared (edge case guard) ──────────────
        if ("STUDENT".equals(user.getRole()) && userDAO.studentNeedsPasswordSetup(user.getEmail())) {
            showError("Your account setup is incomplete. Please set your password.");
            try {
                MainApp.switchScene("/com/elite/erp/fxml/SetPassword.fxml");
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Navigation error: " + ex.getMessage());
            }
            return;
        }

        // ── Route by role ─────────────────────────────────────────────────────
        try {
            switch (user.getRole()) {
                case "ADMIN"      -> MainApp.switchScene("/com/elite/erp/fxml/AdminDashboard.fxml");
                case "HR"         -> MainApp.switchScene("/com/elite/erp/fxml/HRDashboard.fxml");
                case "STUDENT"    -> MainApp.switchScene("/com/elite/erp/fxml/StudentDashboard.fxml");
                case "PARENT"     -> MainApp.switchScene("/com/elite/erp/fxml/ParentDashboard.fxml");
                case "TEACHER"    -> MainApp.switchScene("/com/elite/erp/fxml/TeacherLMS.fxml");
                case "LIBRARIAN"  -> MainApp.switchScene("/com/elite/erp/fxml/LibrarianDashboard.fxml");
                case "NURSE"      -> MainApp.switchScene("/com/elite/erp/fxml/NurseDashboard.fxml");
                default           -> MainApp.switchScene("/com/elite/erp/fxml/Dashboard.fxml");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to load dashboard: " + ex.getMessage());
        }
    }

    @FXML
    private void handleSetPassword() {
        MainApp.switchScene("/com/elite/erp/fxml/SetPassword.fxml");
    }

    @FXML
    private void handleAdminPortal() {
        // Direct admin portal link — pre-fills email
        emailField.setText("admin111@gmail.com");
        emailField.setDisable(false);
        passwordField.requestFocus();
        showError("Admin login: enter your admin password.");
    }

    @FXML
    private void handleApply() {
        MainApp.resetApplication();
        MainApp.switchScene("/com/elite/erp/fxml/Step1.fxml");
    }

    @FXML
    private void handleJoinUs() {
        MainApp.switchScene("/com/elite/erp/fxml/JoinUs.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void shakeCard() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), loginCard);
        shake.setFromX(0); shake.setByX(10); shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
