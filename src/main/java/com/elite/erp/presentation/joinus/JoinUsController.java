package com.elite.erp.presentation.joinus;

import com.elite.erp.MainApp;
import com.elite.erp.dao.StaffRepository;
import com.elite.erp.model.staff.*;
import com.elite.erp.network.NotificationServer;
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

/**
 * JoinUsController — Dynamic multi-role recruitment wizard.
 *
 * OOP: Uses the Staff inheritance hierarchy (Teacher, Nurse, Accountant, Librarian).
 * Generics: Submits via StaffRepository<T extends Staff>.
 * Multithreading: Submit runs on a javafx.concurrent.Task thread.
 * Socket: Broadcasts a real-time alert to the HR Dashboard on submit.
 */
public class JoinUsController implements Initializable {

    // ── Step Labels & Progress ────────────────────────────────────────────────
    @FXML private Label       stepLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label       roleTagLabel;

    // ── Step Panes ────────────────────────────────────────────────────────────
    @FXML private VBox step0, step1, step2, step3, step4;

    // ── Role Selection Cards ──────────────────────────────────────────────────
    @FXML private VBox cardTeacher, cardNurse, cardAccountant, cardLibrarian;
    @FXML private Label roleSelectionError;

    // ── Step 1: Common ────────────────────────────────────────────────────────
    @FXML private TextField fullNameField, emailField, phoneField;

    // ── Step 2: Role Fields ───────────────────────────────────────────────────
    @FXML private VBox teacherFields, nurseFields, accountantFields, librarianFields;
    // Teacher
    @FXML private ComboBox<String> subjectCombo;
    @FXML private Slider           expSlider;
    @FXML private Label            expLabel;
    // Nurse
    @FXML private TextField licenseField, specializationField;
    // Accountant
    @FXML private TextField certField, softwareField;
    // Librarian
    @FXML private TextField libCertField, archiveField, catalogingField;

    // ── Step 3: CV ────────────────────────────────────────────────────────────
    @FXML private VBox  dropZone;
    @FXML private Label cvPathLabel;

    // ── Step 4: Review ────────────────────────────────────────────────────────
    @FXML private TextArea reviewArea;

    // ── Nav Buttons ───────────────────────────────────────────────────────────
    @FXML private Button backBtn, nextBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private int    currentStep = 0;
    private String selectedRole = null;
    private String cvFilePath   = "";

    @SuppressWarnings("rawtypes")
    private final StaffRepository staffRepo = new StaffRepository<>();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        subjectCombo.getItems().addAll(
            "Mathematics", "Science", "History", "English",
            "Physical Education", "Computer Science", "Art", "Music"
        );

        expSlider.valueProperty().addListener((obs, old, val) ->
            expLabel.setText("Years of Experience: " + val.intValue())
        );

        setupDragDrop();
        dropZone.setOnMouseClicked(e -> browseCv());
    }

    // ── Role Selection ────────────────────────────────────────────────────────

    @FXML private void selectTeacher()    { selectRole("TEACHER",    cardTeacher);    }
    @FXML private void selectNurse()      { selectRole("NURSE",      cardNurse);      }
    @FXML private void selectAccountant() { selectRole("ACCOUNTANT", cardAccountant); }
    @FXML private void selectLibrarian()  { selectRole("LIBRARIAN",  cardLibrarian);  }

    private void selectRole(String role, VBox card) {
        selectedRole = role;
        roleTagLabel.setText("📌 " + role);

        // Reset all card borders using CSS classes
        cardTeacher.getStyleClass().removeAll("role-card-active");
        cardNurse.getStyleClass().removeAll("role-card-active");
        cardAccountant.getStyleClass().removeAll("role-card-active");
        cardLibrarian.getStyleClass().removeAll("role-card-active");

        if (!card.getStyleClass().contains("role-card-active")) {
            card.getStyleClass().add("role-card-active");
        }

        roleSelectionError.setVisible(false);
    }

    // ── Wizard Navigation ─────────────────────────────────────────────────────

    @FXML
    private void handleNext() {
        if (!validateCurrentStep()) return;

        if (currentStep == 4) {
            submitApplication();
            return;
        }

        currentStep++;
        updateWizardState();
    }

    @FXML
    private void handleBack() {
        if (currentStep > 0) {
            currentStep--;
            updateWizardState();
        }
    }

    private void updateWizardState() {
        step0.setVisible(currentStep == 0);
        step1.setVisible(currentStep == 1);
        step2.setVisible(currentStep == 2);
        step3.setVisible(currentStep == 3);
        step4.setVisible(currentStep == 4);

        // Show the correct role-specific sub-panel in step 2
        if (currentStep == 2) {
            teacherFields.setVisible("TEACHER".equals(selectedRole));
            nurseFields.setVisible("NURSE".equals(selectedRole));
            accountantFields.setVisible("ACCOUNTANT".equals(selectedRole));
            librarianFields.setVisible("LIBRARIAN".equals(selectedRole));
        }

        int totalSteps = 5;
        stepLabel.setText("Step " + (currentStep + 1) + " of " + totalSteps);
        progressBar.setProgress((currentStep + 1.0) / totalSteps);

        backBtn.setDisable(currentStep == 0);

        if (currentStep == 4) {
            nextBtn.setText("🚀  Submit Application");
            populateReview();
        } else {
            nextBtn.setText("Next Step  ▶");
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateCurrentStep() {
        return switch (currentStep) {
            case 0 -> validateStep0();
            case 1 -> validateStep1();
            case 2 -> validateStep2();
            case 3 -> validateStep3();
            default -> true;
        };
    }

    private boolean validateStep0() {
        if (selectedRole == null) {
            roleSelectionError.setText("Please select a role to continue.");
            roleSelectionError.setVisible(true);
            return false;
        }
        return true;
    }

    private boolean validateStep1() {
        if (fullNameField.getText().trim().isEmpty()) { alert("Please enter your full name."); return false; }
        if (!emailField.getText().matches("^[\\w-\\.]+@gmail\\.com$")) {
            alert("Please enter a valid Gmail address (must end in @gmail.com).");
            return false;
        }
        if (!phoneField.getText().matches("\\d+")) { alert("Phone number must be numeric only."); return false; }
        return true;
    }

    private boolean validateStep2() {
        return switch (selectedRole) {
            case "TEACHER" -> {
                if (subjectCombo.getValue() == null) { alert("Please select a subject."); yield false; }
                yield true;
            }
            case "NURSE" -> {
                if (licenseField.getText().trim().isEmpty()) { alert("Medical license number is required."); yield false; }
                if (specializationField.getText().trim().isEmpty()) { alert("Clinic specialization is required."); yield false; }
                yield true;
            }
            case "ACCOUNTANT" -> {
                if (certField.getText().trim().isEmpty()) { alert("CPA/CMA certification number is required."); yield false; }
                yield true;
            }
            case "LIBRARIAN" -> {
                if (libCertField.getText().trim().isEmpty()) { alert("Library Science Certification is required."); yield false; }
                yield true;
            }
            default -> true;
        };
    }

    private boolean validateStep3() {
        if (cvFilePath.isEmpty()) { alert("Please upload your CV before proceeding."); return false; }
        return true;
    }

    // ── CV Drag & Drop ────────────────────────────────────────────────────────

    private void setupDragDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                cvFilePath = file.getAbsolutePath();
                cvPathLabel.setText("✅ " + file.getName());
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void browseCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Your CV");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(MainApp.getPrimaryStage());
        if (file != null) {
            cvFilePath = file.getAbsolutePath();
            cvPathLabel.setText("✅ " + file.getName());
        }
    }

    // ── Review Populator ──────────────────────────────────────────────────────

    private void populateReview() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════\n");
        sb.append("  ELITE SCHOOL — APPLICATION REVIEW\n");
        sb.append("═══════════════════════════════════\n\n");
        sb.append("Role       : ").append(selectedRole).append("\n");
        sb.append("Full Name  : ").append(fullNameField.getText()).append("\n");
        sb.append("Email      : ").append(emailField.getText()).append("\n");
        sb.append("Phone      : ").append(phoneField.getText()).append("\n\n");

        switch (selectedRole) {
            case "TEACHER" -> sb.append("Subject    : ").append(subjectCombo.getValue()).append("\n")
                                 .append("Experience : ").append((int) expSlider.getValue()).append(" years\n");
            case "NURSE"   -> sb.append("License    : ").append(licenseField.getText()).append("\n")
                                 .append("Specialty  : ").append(specializationField.getText()).append("\n");
            case "ACCOUNTANT" -> sb.append("Cert       : ").append(certField.getText()).append("\n")
                                    .append("Software   : ").append(softwareField.getText()).append("\n");
            case "LIBRARIAN"  -> sb.append("Cert       : ").append(libCertField.getText()).append("\n")
                                    .append("Archives   : ").append(archiveField.getText()).append("\n")
                                    .append("Cataloging : ").append(catalogingField.getText()).append("\n");
        }

        sb.append("\nCV Path    : ").append(cvFilePath).append("\n");
        reviewArea.setText(sb.toString());
    }

    // ── Submission (Multithreading via Task) ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private void submitApplication() {
        nextBtn.setDisable(true);
        nextBtn.setText("Submitting...");

        Staff staff = buildStaffObject();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // JDBC: save to SQLite via Generic Repository
                staffRepo.save(staff);

                // Socket: broadcast real-time alert to HR Dashboard
                if (NotificationServer.isRunning()) {
                    NotificationServer.broadcast(
                        "NEW_APP|" + selectedRole + "|" + staff.getFullName()
                    );
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            alert("✅ Application Submitted!\n\nYour application has been received. "
                + "You will be notified by email once the HR team reviews it.");
            MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            alert("❌ Submission Failed: " + task.getException().getMessage());
            nextBtn.setDisable(false);
            nextBtn.setText("🚀  Submit Application");
        }));

        Thread t = new Thread(task, "StaffSubmit-Thread");
        t.setDaemon(true);
        t.start();
    }

    private Staff buildStaffObject() {
        Staff s;
        switch (selectedRole) {
            case "TEACHER" -> {
                Teacher t = new Teacher();
                t.setSubjectExpertise(subjectCombo.getValue());
                t.setYearsOfExperience((int) expSlider.getValue());
                s = t;
            }
            case "NURSE" -> {
                Nurse n = new Nurse();
                n.setMedicalLicenseNumber(licenseField.getText().trim());
                n.setClinicSpecialization(specializationField.getText().trim());
                s = n;
            }
            case "ACCOUNTANT" -> {
                Accountant a = new Accountant();
                a.setCpaCmaCertification(certField.getText().trim());
                a.setFinancialSoftwareExperience(softwareField.getText().trim());
                s = a;
            }
            case "LIBRARIAN" -> {
                Librarian l = new Librarian();
                l.setLibraryScienceCertification(libCertField.getText().trim());
                l.setArchiveManagementExperience(archiveField.getText().trim());
                l.setCatalogingSystemsKnowledge(catalogingField.getText().trim());
                s = l;
            }
            default -> s = new Teacher(); // fallback
        }
        s.setFullName(fullNameField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setCvPath(cvFilePath);
        s.setStatus("PENDING");
        return s;
    }

    @FXML private void cancel() { MainApp.switchScene("/com/elite/erp/fxml/Login.fxml"); }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
