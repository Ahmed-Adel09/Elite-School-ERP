package com.elite.erp.presentation.admission;

import com.elite.erp.MainApp;
import com.elite.erp.model.Student;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Step3Controller — Student Profile & Academic History.
 *
 * Key changes:
 * - Grade → Year 10/11/12 only (British high school).
 * - ComboBox prompt text forced white via CSS override so it's visible.
 * - DOB age check: flags student as outside 15–19 in model (admin sees it,
 *   user can continue normally — requirement met without blocking UX).
 * - Full state restore when navigating back.
 * - Drag & Drop photo upload with setPreserveRatio(true).
 */
public class Step3Controller implements Initializable {

    @FXML private TextField           firstNameField;
    @FXML private TextField           lastNameField;
    @FXML private DatePicker          dobPicker;
    @FXML private ComboBox<String>    genderCombo;
    @FXML private TextField           nationalityField;
    @FXML private ComboBox<String>    yearCombo;       // renamed grade → year
    @FXML private TextField           prevSchoolField;
    @FXML private TextField           prevGPAField;
    @FXML private TextField           studentEmailField;  // student's own Gmail

    @FXML private StackPane           dropZone;
    @FXML private Label               dropLabel;
    @FXML private ImageView           photoPreview;

    @FXML private Label               errorLabel;

    private String selectedPhotoPath = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ── Populate combos ───────────────────────────────────────────────
        genderCombo.getItems().addAll("Male", "Female", "Prefer not to say");
        yearCombo.getItems().addAll("Year 10", "Year 11", "Year 12");

        errorLabel.setVisible(false);

        // Force combo prompt + selected text to white
        genderCombo.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.55);");
        yearCombo.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.55);");

        // ── Photo preview setup ───────────────────────────────────────────
        photoPreview.setPreserveRatio(true);   // course requirement
        photoPreview.setFitWidth(180);
        photoPreview.setFitHeight(180);

        setupDragAndDrop();

        // ── Restore previously saved data ─────────────────────────────────
        Student s = MainApp.getCurrentApplication().getStudent();
        if (s.getFirstName()    != null) firstNameField.setText(s.getFirstName());
        if (s.getLastName()     != null) lastNameField.setText(s.getLastName());
        if (s.getNationality()  != null) nationalityField.setText(s.getNationality());
        if (s.getPreviousSchool() != null) prevSchoolField.setText(s.getPreviousSchool());
        if (s.getPreviousGPA()    != null) prevGPAField.setText(s.getPreviousGPA());
        if (s.getStudentEmail()   != null) studentEmailField.setText(s.getStudentEmail());

        if (s.getGender() != null) genderCombo.setValue(s.getGender());
        if (s.getApplyingForGrade() != null) yearCombo.setValue(s.getApplyingForGrade());

        if (s.getDateOfBirth() != null) {
            try { dobPicker.setValue(LocalDate.parse(s.getDateOfBirth())); }
            catch (Exception ignored) {}
        }

        if (s.getPhotoPath() != null) {
            selectedPhotoPath = s.getPhotoPath();
            File f = new File(selectedPhotoPath);
            if (f.exists()) {
                photoPreview.setImage(new Image(f.toURI().toString(), true));
                dropLabel.setText("✅ " + f.getName());
            }
        }
    }

    // ── Drag & Drop ───────────────────────────────────────────────────────────
    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles())
                event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        dropZone.setOnDragEntered(event ->
                dropZone.setStyle(dropZone.getStyle()
                        + "-fx-border-color:#C9A84C;-fx-background-color:rgba(201,168,76,0.1);"));
        dropZone.setOnDragExited(event ->
                dropZone.setStyle(dropZone.getStyle()
                        .replace("-fx-border-color:#C9A84C;-fx-background-color:rgba(201,168,76,0.1);", "")));
        dropZone.setOnDragDropped((DragEvent event) -> {
            List<File> files = event.getDragboard().getFiles();
            if (!files.isEmpty()) loadPhoto(files.get(0));
            event.setDropCompleted(true);
            event.consume();
        });
        dropZone.setOnMouseClicked(e -> browsePhoto());
    }

    @FXML
    private void browsePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Student Photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png","*.jpg","*.jpeg","*.gif","*.bmp"));
        File file = fc.showOpenDialog(MainApp.getPrimaryStage());
        if (file != null) loadPhoto(file);
    }

    private void loadPhoto(File file) {
        selectedPhotoPath = file.getAbsolutePath();
        photoPreview.setImage(new Image(file.toURI().toString(), true));
        dropLabel.setText("✅ " + file.getName());
    }

    @FXML
    private void goNext() {
        if (!validate()) return;
        saveToModel();
        MainApp.switchScene("/com/elite/erp/fxml/Step4.fxml");
    }

    @FXML
    private void goBack() {
        saveToModel();   // preserve data even when navigating back
        MainApp.switchScene("/com/elite/erp/fxml/Step2.fxml");
    }

    private void saveToModel() {
        Student s = MainApp.getCurrentApplication().getStudent();
        s.setFirstName(firstNameField.getText().trim());
        s.setLastName(lastNameField.getText().trim());
        if (dobPicker.getValue() != null) {
            s.setDateOfBirth(dobPicker.getValue().toString());

            // Age flag: outside 15–19 range → flag for admin (user NOT blocked)
            int age = LocalDate.now().getYear() - dobPicker.getValue().getYear();
            s.setAgeFlagged(age < 15 || age > 19);
        }
        s.setGender(genderCombo.getValue());
        s.setNationality(nationalityField.getText().trim());
        s.setApplyingForGrade(yearCombo.getValue());
        s.setPreviousSchool(prevSchoolField.getText().trim());
        s.setPreviousGPA(prevGPAField.getText().trim());
        s.setPhotoPath(selectedPhotoPath);
        s.setStudentEmail(studentEmailField.getText().trim());
    }

    private boolean validate() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
            showError("First name and last name are required.");
            return false;
        }
        if (genderCombo.getValue() == null) {
            showError("Please select a gender.");
            return false;
        }
        if (yearCombo.getValue() == null) {
            showError("Please select the applying year.");
            return false;
        }
        String email = studentEmailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid student email address.");
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
