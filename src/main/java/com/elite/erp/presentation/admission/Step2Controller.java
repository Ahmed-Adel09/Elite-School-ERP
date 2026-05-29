package com.elite.erp.presentation.admission;

import com.elite.erp.MainApp;
import com.elite.erp.model.Parent;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Step2Controller — Parent/Guardian Details.
 * - Phone field: digits-only filter (no letters, no special chars).
 * - State preserved: on Back, previously entered data is restored from model.
 * - Corporate Sponsorship toggle animates hidden fields.
 */
public class Step2Controller implements Initializable {

    @FXML private TextField    fatherNameField;
    @FXML private TextField    motherNameField;
    @FXML private TextField    emailField;
    @FXML private TextField    phoneField;
    @FXML private TextField    addressField;
    @FXML private TextField    occupationField;
    @FXML private ToggleButton corporateToggle;
    @FXML private VBox         corporateSection;
    @FXML private TextField    companyNameField;
    @FXML private TextField    companyEmailField;
    @FXML private TextField    sponsorAmountField;
    @FXML private Label        errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        corporateSection.setVisible(false);
        corporateSection.setManaged(false);
        corporateSection.setOpacity(0.0);
        errorLabel.setVisible(false);

        // ── Digits-only filter for phone number ───────────────────────────
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                phoneField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // ── Restore previously entered data (back-navigation) ─────────────
        Parent saved = MainApp.getCurrentApplication().getParent();
        if (saved.getFatherName() != null)  fatherNameField.setText(saved.getFatherName());
        if (saved.getMotherName() != null)  motherNameField.setText(saved.getMotherName());
        if (saved.getEmail()      != null)  emailField.setText(saved.getEmail());
        if (saved.getPhone()      != null)  phoneField.setText(saved.getPhone());
        if (saved.getAddress()    != null)  addressField.setText(saved.getAddress());
        if (saved.getOccupation() != null)  occupationField.setText(saved.getOccupation());

        if (saved.isCorporateSponsor()) {
            corporateToggle.setSelected(true);
            corporateToggle.setText("✅ Corporate Sponsor: ON");
            corporateSection.setVisible(true);
            corporateSection.setManaged(true);
            corporateSection.setOpacity(1.0);
            if (saved.getCompanyName()  != null) companyNameField.setText(saved.getCompanyName());
            if (saved.getCompanyEmail() != null) companyEmailField.setText(saved.getCompanyEmail());
            if (saved.getSponsorAmount() > 0)
                sponsorAmountField.setText(String.valueOf(saved.getSponsorAmount()));
        }
    }

    @FXML
    private void toggleCorporate() {
        boolean show = corporateToggle.isSelected();
        corporateToggle.setText(show ? "✅ Corporate Sponsor: ON" : "🏢 Corporate Sponsorship");

        if (show) {
            corporateSection.setVisible(true);
            corporateSection.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(350), corporateSection);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), corporateSection);
            tt.setFromY(-15); tt.setToY(0);
            new ParallelTransition(ft, tt).play();
        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(250), corporateSection);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                corporateSection.setVisible(false);
                corporateSection.setManaged(false);
            });
            ft.play();
        }
    }

    @FXML
    private void goNext() {
        if (!validate()) return;
        saveToModel();
        MainApp.switchScene("/com/elite/erp/fxml/Step3.fxml");
    }

    @FXML
    private void goBack() {
        saveToModel();   // preserve even when going back
        MainApp.switchScene("/com/elite/erp/fxml/Step1.fxml");
    }

    private void saveToModel() {
        Parent parent = MainApp.getCurrentApplication().getParent();
        parent.setFatherName(fatherNameField.getText().trim());
        parent.setMotherName(motherNameField.getText().trim());
        parent.setEmail(emailField.getText().trim());
        parent.setPhone(phoneField.getText().trim());
        parent.setAddress(addressField.getText().trim());
        parent.setOccupation(occupationField.getText().trim());
        parent.setCorporateSponsor(corporateToggle.isSelected());

        if (corporateToggle.isSelected()) {
            parent.setCompanyName(companyNameField.getText().trim());
            parent.setCompanyEmail(companyEmailField.getText().trim());
            try {
                parent.setSponsorAmount(Double.parseDouble(sponsorAmountField.getText().trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private boolean validate() {
        if (fatherNameField.getText().isBlank() && motherNameField.getText().isBlank()) {
            showError("Please enter at least one parent name.");
            return false;
        }
        if (emailField.getText().isBlank()) {
            showError("Email address is required.");
            return false;
        }
        if (!emailField.getText().matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return false;
        }
        errorLabel.setVisible(false);
        return true;
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }
}
