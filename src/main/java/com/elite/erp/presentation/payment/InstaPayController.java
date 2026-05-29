package com.elite.erp.presentation.payment;

import com.elite.erp.MainApp;
import com.elite.erp.business.AdmissionService;
import com.elite.erp.business.PaymentService;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.Transaction;
import com.elite.erp.network.NotificationServer;
import com.elite.erp.util.Response;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * InstaPayController — Premium InstaPay payment screen.
 *
 * Demonstrates:
 * - Multithreading: verification runs in a background Task
 * - Sockets: on success, broadcasts to NotificationServer
 * - setPreserveRatio(true) on QR ImageView (per course requirement)
 */
public class InstaPayController implements Initializable {

    @FXML private Label         ipaAddressLabel;
    @FXML private Button        copyIpaButton;
    @FXML private StackPane     qrContainer;       // fixed-size container
    @FXML private ImageView     qrImageView;       // preserveRatio=true
    @FXML private Label         amountLabel;
    @FXML private Label         summaryLabel;
    @FXML private TextField     referenceField;
    @FXML private Button        verifyButton;
    @FXML private ProgressIndicator verifySpinner;
    @FXML private Label         statusLabel;
    @FXML private VBox          successPanel;
    @FXML private Label         txRefLabel;
    @FXML private Label         errorLabel;
    @FXML private Button        finalizeButton;

    private final PaymentService   paymentService   = new PaymentService();
    private final AdmissionService admissionService = new AdmissionService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Display school IPA
        ipaAddressLabel.setText(PaymentService.SCHOOL_IPA);

        // Amount
        amountLabel.setText(paymentService.getAmountDisplay());

        // Student summary — null-safe (app may be partially filled)
        AdmissionApplication app = MainApp.getCurrentApplication();
        if (app != null && app.getStudent() != null) {
            String name  = app.getStudent().getFullName();
            String grade = app.getStudent().getApplyingForGrade();
            summaryLabel.setText(
                    (name  != null && !name.isBlank()  ? name  : "Student")
                    + "  •  "
                    + (grade != null && !grade.isBlank() ? grade : "—"));
        } else {
            summaryLabel.setText("Admission Application");
        }

        // QR code image — CRITICAL: preserveRatio=true inside fixed StackPane
        qrImageView.setPreserveRatio(true);
        qrImageView.setFitWidth(160);
        qrImageView.setFitHeight(160);

        // Initial UI state — set in code, NOT FXML, to avoid LoadException
        verifySpinner.setVisible(false);
        statusLabel.setVisible(false);
        successPanel.setVisible(false);
        successPanel.setManaged(false);
        errorLabel.setVisible(false);
        finalizeButton.setDisable(true);

        // Entrance animation
        FadeTransition ft = new FadeTransition(Duration.millis(600), qrContainer);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML
    private void copyIPA() {
        ClipboardContent content = new ClipboardContent();
        content.putString(PaymentService.SCHOOL_IPA);
        Clipboard.getSystemClipboard().setContent(content);
        copyIpaButton.setText("✅ Copied!");
        PauseTransition pt = new PauseTransition(Duration.seconds(2));
        pt.setOnFinished(e -> copyIpaButton.setText("📋 Copy"));
        pt.play();
    }

    /** Runs verification in a background thread — Multithreading requirement */
    @FXML
    private void handleVerify() {
        String ref = referenceField.getText().trim();
        errorLabel.setVisible(false);
        statusLabel.setVisible(false);

        if (ref.isBlank()) {
            showError("Please enter your InstaPay transaction reference number.");
            return;
        }

        // Lock UI, show spinner
        verifyButton.setDisable(true);
        referenceField.setDisable(true);
        verifySpinner.setVisible(true);

        int appId = MainApp.getCurrentApplication().getId();

        Task<Response<Transaction>> verifyTask = new Task<>() {
            @Override
            protected Response<Transaction> call() throws Exception {
                updateMessage("Contacting InstaPay servers...");
                Thread.sleep(1800); // simulate network call
                updateMessage("Verifying reference number...");
                Thread.sleep(800);
                return paymentService.verifyPayment(appId, ref);
            }
        };

        verifyTask.setOnSucceeded(e -> {
            verifySpinner.setVisible(false);
            Response<Transaction> result = verifyTask.getValue();
            if (result.isSuccess()) {
                showSuccess(result.getData());
                MainApp.getCurrentApplication().setPaymentCompleted(true);
            } else {
                showError(result.getMessage());
                verifyButton.setDisable(false);
                referenceField.setDisable(false);
            }
        });

        verifyTask.setOnFailed(e -> {
            verifySpinner.setVisible(false);
            showError("Verification failed: " + verifyTask.getException().getMessage());
            verifyButton.setDisable(false);
            referenceField.setDisable(false);
        });

        Thread t = new Thread(verifyTask, "PaymentVerify-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void showSuccess(Transaction tx) {
        txRefLabel.setText("Reference: " + tx.getIpaReference()
                + "  •  $" + String.format("%.2f", tx.getAmount()) + " Verified");
        successPanel.setManaged(true);
        successPanel.setVisible(true);
        successPanel.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), successPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        finalizeButton.setDisable(false);

        statusLabel.setText("✅ Payment Verified! You may now submit your application.");
        statusLabel.setStyle("-fx-text-fill: #4CD97B;");
        statusLabel.setVisible(true);
    }

    @FXML
    private void handleFinalize() {
        Response<AdmissionApplication> result =
                admissionService.submitApplication(MainApp.getCurrentApplication());

        if (result.isSuccess()) {
            int id = result.getData().getId();
            // Broadcast to admin via socket
            NotificationServer.broadcast("📋 NEW APPLICATION #" + id + " — "
                    + MainApp.getCurrentApplication().getStudent().getFullName()
                    + " | Grade: " + MainApp.getCurrentApplication().getStudent().getApplyingForGrade()
                    + " | Payment: VERIFIED");
            MainApp.resetApplication();
            MainApp.switchScene("/com/elite/erp/fxml/SubmitSuccess.fxml");
        } else {
            showError("Submission failed: " + result.getMessage());
        }
    }

    @FXML private void goBack() { MainApp.switchScene("/com/elite/erp/fxml/PaymentChoice.fxml"); }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }
}
