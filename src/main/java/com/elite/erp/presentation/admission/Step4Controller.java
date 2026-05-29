package com.elite.erp.presentation.admission;

import com.elite.erp.MainApp;
import com.elite.erp.model.Student;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Step4Controller — Medical & Wellbeing information.
 * - State restore: fields re-populated if user navigates back from InstaPay.
 * - TextArea text colour inherits from CSS (navy bg, white text) — no overrides.
 */
public class Step4Controller implements Initializable {

    @FXML private TextArea allergyArea;
    @FXML private TextArea medicalNotesArea;
    @FXML private Label    vaccinationLabel;
    @FXML private Button   browseVaccinationBtn;
    @FXML private Label    errorLabel;

    private String vaccinationFilePath = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);

        // Restore previously entered medical data (back navigation or reload)
        Student s = MainApp.getCurrentApplication().getStudent();
        if (s.getAllergies()    != null && !s.getAllergies().isBlank())
            allergyArea.setText(s.getAllergies());
        if (s.getMedicalNotes() != null && !s.getMedicalNotes().isBlank())
            medicalNotesArea.setText(s.getMedicalNotes());
        if (s.getVaccinationRecordPath() != null) {
            vaccinationFilePath = s.getVaccinationRecordPath();
            vaccinationLabel.setText("📄 " + new File(vaccinationFilePath).getName());
        }
    }

    @FXML
    private void browseVaccinationRecord() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Vaccination Record");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf","*.jpg","*.jpeg","*.png","*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(MainApp.getPrimaryStage());
        if (file != null) {
            vaccinationFilePath = file.getAbsolutePath();
            vaccinationLabel.setText("📄 " + file.getName());
        }
    }

    @FXML
    private void goNext() {
        saveToModel();
        MainApp.switchScene("/com/elite/erp/fxml/PaymentChoice.fxml");
    }

    @FXML
    private void goBack() {
        saveToModel();   // save even when going back
        MainApp.switchScene("/com/elite/erp/fxml/Step3.fxml");
    }

    private void saveToModel() {
        Student s = MainApp.getCurrentApplication().getStudent();
        s.setAllergies(allergyArea.getText().trim());
        s.setMedicalNotes(medicalNotesArea.getText().trim());
        s.setVaccinationRecordPath(vaccinationFilePath);
    }
}
