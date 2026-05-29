package com.elite.erp.presentation.login;

import com.elite.erp.MainApp;
import com.elite.erp.dao.UserDAO;
import com.elite.erp.util.Response;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SetPasswordController implements Initializable {

    @FXML private VBox mainCard;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        FadeTransition fade = new FadeTransition(Duration.millis(800), mainCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), mainCard);
        slide.setFromY(30);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();

        statusLabel.setVisible(false);
    }

    @FXML
    private void handleSetPassword() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();
        String conf  = confirmPasswordField.getText();

        if (email.isEmpty() || pass.isEmpty() || conf.isEmpty()) {
            showStatus("All fields are required.", true);
            return;
        }

        if (!pass.equals(conf)) {
            showStatus("Passwords do not match.", true);
            return;
        }

        if (pass.length() < 6) {
            showStatus("Password must be at least 6 characters.", true);
            return;
        }

        Response<Boolean> resp = userDAO.setPassword(email, pass);
        if (resp.isSuccess()) {
            showStatus("✅ Password set successfully! Redirecting...", false);
            // Delay to let the user see the success message
            FadeTransition ft = new FadeTransition(Duration.millis(1500), mainCard);
            ft.setOnFinished(e -> MainApp.switchScene("/com/elite/erp/fxml/Login.fxml"));
            ft.play();
        } else {
            showStatus("Error: " + resp.getMessage(), true);
            shakeCard();
        }
    }

    @FXML
    private void handleBack() {
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #FF6B6B;" : "-fx-text-fill: #4CD97B;");
        statusLabel.setVisible(true);
    }

    private void shakeCard() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), mainCard);
        shake.setFromX(0); shake.setByX(10); shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
