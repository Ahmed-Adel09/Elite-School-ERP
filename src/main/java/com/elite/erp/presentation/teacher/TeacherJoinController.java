package com.elite.erp.presentation.teacher;

import com.elite.erp.MainApp;
import com.elite.erp.dao.TeacherDAO;
import com.elite.erp.model.TeacherApplication;
import com.elite.erp.network.NotificationServer;
import com.elite.erp.util.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class TeacherJoinController implements Initializable {

    @FXML private Label stepLabel;
    @FXML private ProgressBar progressBar;
    
    @FXML private VBox step1;
    @FXML private VBox step2;
    @FXML private VBox step3;
    @FXML private VBox step4;

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private ComboBox<String> subjectCombo;
    @FXML private Slider experienceSlider;
    @FXML private Label experienceLabel;

    @FXML private VBox dropZone;
    @FXML private Label cvPathLabel;

    @FXML private TextArea reviewArea;

    @FXML private Button backBtn;
    @FXML private Button nextBtn;

    private int currentStep = 1;
    private String cvFilePath = "";

    private final TeacherDAO dao = new TeacherDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        subjectCombo.getItems().addAll("Mathematics", "Science", "History", "English", "Physical Education", "Computer Science");

        experienceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            experienceLabel.setText("Years of Experience: " + newVal.intValue());
        });

        // Setup Drag & Drop
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                cvFilePath = file.getAbsolutePath();
                cvPathLabel.setText("Selected: " + file.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        dropZone.setOnMouseClicked(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = chooser.showOpenDialog(MainApp.getPrimaryStage());
            if (file != null) {
                cvFilePath = file.getAbsolutePath();
                cvPathLabel.setText("Selected: " + file.getName());
            }
        });
    }

    @FXML
    private void handleNext() {
        if (currentStep == 1 && !validateStep1()) return;
        if (currentStep == 2 && !validateStep2()) return;
        if (currentStep == 3 && !validateStep3()) return;
        
        if (currentStep == 4) {
            submitApplication();
            return;
        }

        currentStep++;
        updateWizardState();
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateWizardState();
        }
    }

    private void updateWizardState() {
        step1.setVisible(currentStep == 1);
        step2.setVisible(currentStep == 2);
        step3.setVisible(currentStep == 3);
        step4.setVisible(currentStep == 4);

        stepLabel.setText("Step " + currentStep + " of 4");
        progressBar.setProgress(currentStep / 4.0);

        backBtn.setDisable(currentStep == 1);

        if (currentStep == 4) {
            nextBtn.setText("Submit Application");
            populateReview();
        } else {
            nextBtn.setText("Next Step");
        }
    }

    private boolean validateStep1() {
        String email = emailField.getText();
        String phone = phoneField.getText();
        
        if (fullNameField.getText().trim().isEmpty()) {
            showAlert("Validation", "Please enter your full name.");
            return false;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Validation", "Please enter a valid email address.");
            return false;
        }
        if (!phone.matches("\\d+")) {
            showAlert("Validation", "Phone number must contain only numeric digits.");
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (subjectCombo.getValue() == null) {
            showAlert("Validation", "Please select a subject of expertise.");
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if (cvFilePath.isEmpty()) {
            showAlert("Validation", "Please upload your CV before proceeding.");
            return false;
        }
        return true;
    }

    private void populateReview() {
        reviewArea.setText(
            "Name: " + fullNameField.getText() + "\n" +
            "Email: " + emailField.getText() + "\n" +
            "Phone: " + phoneField.getText() + "\n\n" +
            "Subject: " + subjectCombo.getValue() + "\n" +
            "Experience: " + (int) experienceSlider.getValue() + " years\n\n" +
            "CV Path: " + cvFilePath
        );
    }

    private void submitApplication() {
        nextBtn.setDisable(true);
        nextBtn.setText("Submitting...");

        TeacherApplication app = new TeacherApplication();
        app.setFullName(fullNameField.getText());
        app.setEmail(emailField.getText());
        app.setPhone(phoneField.getText());
        app.setSubject(subjectCombo.getValue());
        app.setExperienceYears((int) experienceSlider.getValue());
        app.setCvPath(cvFilePath);
        app.setStatus("PENDING");

        // Multithreading requirement: javafx.concurrent.Task
        Task<Void> submissionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Save to DB
                Response<TeacherApplication> resp = dao.save(app);
                
                if (resp.isSuccess()) {
                    // Real-time Notification via Sockets
                    if (NotificationServer.isRunning()) {
                        NotificationServer.broadcast("New Teacher Application from " + app.getFullName());
                    }
                    Platform.runLater(() -> {
                        showAlert("Success", "Application submitted successfully!");
                        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Error", resp.getMessage());
                        nextBtn.setDisable(false);
                        nextBtn.setText("Submit Application");
                    });
                }
                return null;
            }
        };

        new Thread(submissionTask).start();
    }

    @FXML
    private void cancel() {
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
