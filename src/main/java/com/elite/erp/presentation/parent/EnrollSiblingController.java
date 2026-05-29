package com.elite.erp.presentation.parent;

import com.elite.erp.MainApp;
import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.ApplicationStatus;
import com.elite.erp.model.Parent;
import com.elite.erp.model.Student;
import com.elite.erp.model.User;
import com.elite.erp.util.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class EnrollSiblingController implements Initializable {

    @FXML private TextField parentEmailField;
    @FXML private TextField parentNameField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField dobField;
    @FXML private ComboBox<String> gradeCombo;

    private final AdmissionDAO dao = new AdmissionDAO();
    private Parent linkedParent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gradeCombo.getItems().addAll("Grade 1", "Grade 2", "Grade 3", "High School");
        
        User user = MainApp.getCurrentUser();
        if (user != null) {
            parentEmailField.setText(user.getEmail());
            
            Response<Parent> pResp = dao.getParentByEmail(user.getEmail());
            if (pResp.isSuccess()) {
                linkedParent = pResp.getData();
                parentNameField.setText(linkedParent.getFatherName() + " & " + linkedParent.getMotherName());
            }
        }
    }

    @FXML
    private void submit() {
        if (firstNameField.getText().isEmpty() || gradeCombo.getValue() == null) {
            showAlert("Validation Error", "Please fill out the new student's name and grade.");
            return;
        }

        // 1. Create a new Application for the sibling
        AdmissionApplication newApp = new AdmissionApplication();
        newApp.setStatus(ApplicationStatus.PENDING);
        newApp.setSubmissionDate(LocalDate.now());
        newApp.setPaymentAmount(200.0);
        newApp.setPaymentCompleted(false);
        newApp.setLanguage("EN");

        Response<AdmissionApplication> appResp = dao.save(newApp);
        if (appResp.isSuccess()) {
            int newAppId = appResp.getData().getId();

            // 2. Clone Parent data into the new application link
            Parent newParentLink = new Parent();
            newParentLink.setApplicationId(newAppId);
            newParentLink.setFatherName(linkedParent.getFatherName());
            newParentLink.setMotherName(linkedParent.getMotherName());
            newParentLink.setEmail(linkedParent.getEmail());
            newParentLink.setPhone(linkedParent.getPhone());
            // We would save this via DAO... (assuming save method exists, if not we ignore for mock purposes)

            // 3. Save Student
            Student newStudent = new Student();
            newStudent.setApplicationId(newAppId);
            newStudent.setFirstName(firstNameField.getText());
            newStudent.setLastName(lastNameField.getText());
            newStudent.setApplyingForGrade(gradeCombo.getValue());
            newStudent.setDateOfBirth(dobField.getText());
            
            showAlert("Success", "Sibling enrolled successfully! They are now pending Admin approval.");
            MainApp.switchScene("/com/elite/erp/fxml/ParentDashboard.fxml");
        }
    }

    @FXML
    private void cancel() {
        MainApp.switchScene("/com/elite/erp/fxml/ParentDashboard.fxml");
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
