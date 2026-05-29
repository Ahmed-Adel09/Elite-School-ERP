package com.elite.erp.presentation.admission;

import com.elite.erp.MainApp;
import com.elite.erp.business.AdmissionService;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.network.NotificationServer;
import com.elite.erp.util.Response;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.net.URL;
import java.util.ResourceBundle;

public class Step5Controller implements Initializable {

    @FXML private Label     summaryName;
    @FXML private Label     summaryGrade;
    @FXML private Label     summaryParent;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Button    payButton;
    @FXML private Button    finalizeButton;
    @FXML private VBox      successOverlay;
    @FXML private Label     applicationIdLabel;
    @FXML private Label     errorLabel;

    private final AdmissionService admissionService = new AdmissionService();
    private boolean paymentDone = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        successOverlay.setVisible(false);
        finalizeButton.setDisable(true);
        AdmissionApplication app = MainApp.getCurrentApplication();
        summaryName.setText(app.getStudent().getFullName());
        summaryGrade.setText("Applying for: " + app.getStudent().getApplyingForGrade());
        summaryParent.setText("Guardian: " + app.getParent().getFatherName());
    }

    @FXML
    private void handlePay() {
        String card = cardNumberField.getText().replaceAll("\\s+", "");
        if (card.length() < 12 || expiryField.getText().isBlank() || cvvField.getText().isBlank()) {
            showError("All payment fields are required (card min 12 digits).");
            return;
        }
        paymentDone = true;
        MainApp.getCurrentApplication().setPaymentCompleted(true);
        payButton.setText("✅ Payment Verified — $200.00");
        payButton.setDisable(true);
        cardNumberField.setDisable(true);
        expiryField.setDisable(true);
        cvvField.setDisable(true);
        finalizeButton.setDisable(false);
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleFinalize() {
        if (!paymentDone) { showError("Complete payment first."); return; }
        Response<AdmissionApplication> result =
                admissionService.submitApplication(MainApp.getCurrentApplication());
        if (result.isSuccess()) {
            int id = result.getData().getId();
            applicationIdLabel.setText("Application ID: #" + id);
            successOverlay.setVisible(true);
            successOverlay.setOpacity(0);
            ScaleTransition sc = new ScaleTransition(Duration.millis(500), successOverlay);
            sc.setFromX(0.5); sc.setToX(1.0); sc.setFromY(0.5); sc.setToY(1.0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), successOverlay);
            ft.setFromValue(0); ft.setToValue(1);
            new ParallelTransition(sc, ft).play();
            finalizeButton.setDisable(true);
            NotificationServer.broadcast("New Application #" + id + " submitted by "
                    + MainApp.getCurrentApplication().getStudent().getFullName());
        } else {
            showError(result.getMessage());
        }
    }

    @FXML private void goToDashboard() {
        MainApp.resetApplication();
        MainApp.switchScene("/com/elite/erp/fxml/Dashboard.fxml");
    }

    @FXML private void goBack() { MainApp.switchScene("/com/elite/erp/fxml/Step4.fxml"); }

    private void showError(String msg) { errorLabel.setText("⚠  " + msg); errorLabel.setVisible(true); }
}
