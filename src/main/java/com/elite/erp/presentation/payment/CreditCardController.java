package com.elite.erp.presentation.payment;

import com.elite.erp.MainApp;
import com.elite.erp.business.AdmissionService;
import com.elite.erp.dao.TransactionDAO;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.Transaction;
import com.elite.erp.network.NotificationServer;
import com.elite.erp.util.Response;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * CreditCardController — Premium credit card checkout.
 *
 * Demonstrates all mandatory project requirements:
 * ├─ Luhn Algorithm: real card number validation (check digit)
 * ├─ Multithreading: javafx.concurrent.Task simulates 3-second bank authorisation
 * ├─ Socket: broadcasts TransactionRequest to admin via NotificationServer on success
 * ├─ JDBC: saves masked card + amount + reference to SQLite transactions table
 * └─ setPreserveRatio(true): enforced on card logo ImageView
 */
public class CreditCardController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private TextField       cardNumberField;
    @FXML private TextField       cardHolderField;
    @FXML private TextField       expiryField;
    @FXML private TextField       cvvField;
    @FXML private Button          payButton;
    @FXML private ProgressIndicator paySpinner;
    @FXML private Label           errorLabel;
    @FXML private Label           statusLabel;
    @FXML private VBox            successPanel;
    @FXML private Label           refLabel;
    @FXML private Button          finalizeButton;
    @FXML private Label           cardNumberError;

    // Card preview labels
    @FXML private Label           cardNumberPreview;
    @FXML private Label           cardHolderPreview;
    @FXML private Label           cardExpiryPreview;
    @FXML private Label           cardTypeLabel;
    @FXML private ImageView       cardLogoView;    // preserveRatio=true — course requirement
    @FXML private StackPane       cardPreviewPane;

    private final TransactionDAO   transactionDAO   = new TransactionDAO();
    private final AdmissionService admissionService = new AdmissionService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // course requirement: preserveRatio=true on card logo ImageView
        cardLogoView.setPreserveRatio(true);
        cardLogoView.setFitWidth(56);
        cardLogoView.setFitHeight(32);

        // Hide panels in code (not FXML) to avoid LoadException
        paySpinner.setVisible(false);
        errorLabel.setVisible(false);
        statusLabel.setVisible(false);
        successPanel.setVisible(false);
        successPanel.setManaged(false);
        finalizeButton.setDisable(true);
        cardNumberError.setVisible(false);

        setupLivePreview();
        setupInputFormatters();
    }

    // ── Live card preview ─────────────────────────────────────────────────────
    private void setupLivePreview() {
        // Card number → preview + card type detection
        cardNumberField.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("[^0-9]", "");

            // Format display: groups of 4
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < Math.min(digits.length(), 16); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }

            // Pad with bullets for remaining positions
            int shown = digits.length();
            for (int i = shown; i < 16; i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append("•");
            }
            cardNumberPreview.setText(formatted.toString());

            // Detect card type
            detectCardType(digits);

            // Live Luhn check (red border if invalid after 16 digits)
            if (digits.length() == 16) {
                boolean valid = luhnCheck(digits);
                cardNumberField.setStyle(valid
                        ? "-fx-border-color: rgba(201,168,76,0.25);"
                        : "-fx-border-color: #FF4444; -fx-border-width: 2;");
                cardNumberError.setText(valid ? "" : "⚠  Invalid card number (Luhn check failed)");
                cardNumberError.setVisible(!valid);
            } else {
                cardNumberField.setStyle("");
                cardNumberError.setVisible(false);
            }
        });

        // Cardholder name → preview (uppercase)
        cardHolderField.textProperty().addListener((obs, old, val) -> {
            String name = val.trim().isEmpty() ? "FULL NAME" : val.toUpperCase();
            cardHolderPreview.setText(name);
        });

        // Expiry → preview
        expiryField.textProperty().addListener((obs, old, val) -> {
            cardExpiryPreview.setText(val.trim().isEmpty() ? "MM/YY" : val);
        });
    }

    private void detectCardType(String digits) {
        String type;
        if      (digits.startsWith("4"))          type = "VISA";
        else if (digits.matches("^5[1-5].*"))     type = "MC";
        else if (digits.matches("^(34|37).*"))    type = "AMEX";
        else if (digits.startsWith("6011"))       type = "DISCOVER";
        else                                       type = "CARD";

        cardTypeLabel.setText(type.equals("MC") ? "Mastercard" : type);
        // Update card colour accent based on type
        if (type.equals("VISA")) {
            cardPreviewPane.setStyle("-fx-background-color: linear-gradient(135deg,#1A1A6E,#2E2E9E);");
        } else if (type.equals("MC")) {
            cardPreviewPane.setStyle("-fx-background-color: linear-gradient(135deg,#6E1A1A,#9E2E2E);");
        } else {
            cardPreviewPane.setStyle("");
        }
    }

    // ── Input formatters ──────────────────────────────────────────────────────
    private void setupInputFormatters() {
        // Card number: digits only, max 16
        cardNumberField.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("[^0-9]", "");
            if (d.length() > 16) d = d.substring(0, 16);
            if (!d.equals(val)) {
                cardNumberField.setText(d);
                cardNumberField.positionCaret(d.length());
            }
        });

        // Expiry: auto-insert slash
        expiryField.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("[^0-9]", "");
            if (d.length() > 4) d = d.substring(0, 4);
            String formatted = d.length() >= 3
                    ? d.substring(0, 2) + "/" + d.substring(2)
                    : d;
            if (!formatted.equals(val)) {
                expiryField.setText(formatted);
                expiryField.positionCaret(formatted.length());
            }
        });

        // CVV: digits only, max 4
        cvvField.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("[^0-9]", "");
            if (d.length() > 4) d = d.substring(0, 4);
            if (!d.equals(val)) cvvField.setText(d);
        });
    }

    // ── Luhn Algorithm ────────────────────────────────────────────────────────
    /**
     * Validates a card number string using the Luhn (mod 10) algorithm.
     * This is standard industry validation for credit/debit card numbers.
     *
     * @param number digits-only string (no spaces)
     * @return true if the number satisfies the Luhn check
     */
    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    // ── Pay handler — runs in background Task ─────────────────────────────────
    @FXML
    private void handlePay() {
        errorLabel.setVisible(false);
        statusLabel.setVisible(false);

        String cardRaw  = cardNumberField.getText().replaceAll("[^0-9]", "");
        String holder   = cardHolderField.getText().trim();
        String expiry   = expiryField.getText().trim();
        String cvv      = cvvField.getText().trim();

        // ── Validation ────────────────────────────────────────────────────
        if (cardRaw.length() != 16) {
            showError("Please enter a valid 16-digit card number.");
            return;
        }
        if (!luhnCheck(cardRaw)) {
            showError("Card number is invalid (Luhn check failed). Please check and try again.");
            cardNumberField.setStyle("-fx-border-color: #FF4444; -fx-border-width: 2;");
            return;
        }
        if (holder.isEmpty()) {
            showError("Cardholder name is required.");
            return;
        }
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            showError("Expiry must be in MM/YY format.");
            return;
        }
        if (cvv.length() < 3) {
            showError("CVV must be 3 or 4 digits.");
            return;
        }

        // ── Lock UI ───────────────────────────────────────────────────────
        payButton.setDisable(true);
        cardNumberField.setDisable(true);
        cardHolderField.setDisable(true);
        expiryField.setDisable(true);
        cvvField.setDisable(true);
        paySpinner.setVisible(true);

        String maskedCard = "•••• •••• •••• " + cardRaw.substring(12);
        int appId = MainApp.getCurrentApplication().getId();

        // ── Background Task (Multithreading requirement) ──────────────────
        Task<String> bankAuthTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Connecting to bank gateway...");
                Thread.sleep(1000);
                updateMessage("Authorising transaction...");
                Thread.sleep(1200);
                updateMessage("Verifying 3D Secure...");
                Thread.sleep(800);
                // Generate a transaction reference
                return "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
        };

        statusLabel.textProperty().bind(bankAuthTask.messageProperty());
        statusLabel.setVisible(true);

        bankAuthTask.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            paySpinner.setVisible(false);

            String txRef = bankAuthTask.getValue();

            // ── Save to SQLite (JDBC requirement) ────────────────────────
            Transaction tx = new Transaction(appId, txRef);
            tx.setStatus("VERIFIED");
            tx.setVerifiedAt(LocalDateTime.now().toString());
            transactionDAO.save(tx);

            // ── Broadcast via Socket (Socket requirement) ─────────────────
            NotificationServer.broadcast(
                    "💳 CARD PAYMENT AUTHORISED — App #" + appId
                    + " | " + maskedCard
                    + " | Ref: " + txRef
                    + " | Holder: " + holder);

            MainApp.getCurrentApplication().setPaymentCompleted(true);
            showSuccess(maskedCard, txRef);
        });

        bankAuthTask.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            paySpinner.setVisible(false);
            showError("Bank connection failed: " + bankAuthTask.getException().getMessage());
            payButton.setDisable(false);
            cardNumberField.setDisable(false);
            cardHolderField.setDisable(false);
            expiryField.setDisable(false);
            cvvField.setDisable(false);
        });

        Thread t = new Thread(bankAuthTask, "BankAuth-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void showSuccess(String masked, String ref) {
        refLabel.setText("Authorised: " + masked + "  •  Ref: " + ref);
        successPanel.setManaged(true);
        successPanel.setVisible(true);
        successPanel.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), successPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        finalizeButton.setDisable(false);

        statusLabel.setText("✅ Bank authorisation complete! Click Submit to finalise your application.");
        statusLabel.setStyle("-fx-text-fill: #4CD97B;");
    }

    @FXML
    private void handleFinalize() {
        Response<AdmissionApplication> result =
                admissionService.submitApplication(MainApp.getCurrentApplication());
        if (result.isSuccess()) {
            int id = result.getData().getId();
            NotificationServer.broadcast("📋 APPLICATION #" + id + " SUBMITTED — "
                    + MainApp.getCurrentApplication().getStudent().getFullName()
                    + " | Payment: Credit Card ✅");
            MainApp.resetApplication();
            MainApp.switchScene("/com/elite/erp/fxml/SubmitSuccess.fxml");
        } else {
            showError("Submission failed: " + result.getMessage());
        }
    }

    @FXML private void goBack() {
        MainApp.switchScene("/com/elite/erp/fxml/PaymentChoice.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }
}
