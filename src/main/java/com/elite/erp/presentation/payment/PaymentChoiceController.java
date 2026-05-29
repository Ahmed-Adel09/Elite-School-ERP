package com.elite.erp.presentation.payment;

import com.elite.erp.MainApp;
import com.elite.erp.model.AdmissionApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * PaymentChoiceController — routes user to InstaPay or Credit Card.
 */
public class PaymentChoiceController implements Initializable {

    @FXML private Label summaryLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        AdmissionApplication app = MainApp.getCurrentApplication();
        if (app != null && app.getStudent() != null) {
            String name  = app.getStudent().getFullName();
            String grade = app.getStudent().getApplyingForGrade();
            summaryLabel.setText(
                    (name  != null && !name.isBlank()  ? name  : "Student")
                    + "  •  "
                    + (grade != null && !grade.isBlank() ? grade : "—")
                    + "  •  Admission Fee: $200.00");
        } else {
            summaryLabel.setText("Admission Fee: $200.00");
        }
    }

    @FXML private void chooseInstaPay() {
        MainApp.switchScene("/com/elite/erp/fxml/InstaPayStep.fxml");
    }

    @FXML private void chooseCreditCard() {
        MainApp.switchScene("/com/elite/erp/fxml/CreditCardView.fxml");
    }

    @FXML private void goBack() {
        MainApp.switchScene("/com/elite/erp/fxml/Step4.fxml");
    }
}
