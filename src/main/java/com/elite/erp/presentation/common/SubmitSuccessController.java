package com.elite.erp.presentation.common;

import com.elite.erp.MainApp;
import javafx.fxml.FXML;

/**
 * Controller for the post-submission success screen.
 */
public class SubmitSuccessController {

    @FXML
    private void returnToLogin() {
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }
}
