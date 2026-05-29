package com.elite.erp.presentation.hr;

import com.elite.erp.MainApp;
import com.elite.erp.business.EmailService;
import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.dao.StaffRepository;
import com.elite.erp.dao.UserDAO;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.ApplicationStatus;
import com.elite.erp.model.User;
import com.elite.erp.model.staff.Staff;
import com.elite.erp.network.NotificationClient;
import com.elite.erp.util.Response;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.elite.erp.dao.DatabaseManager;
import java.util.List;
import java.util.ResourceBundle;

/**
 * HRDashboardController — Master inbox for all HR recruitment activity.
 *
 * Demonstrates all mandatory project requirements:
 *  - Generics:        StaffRepository<Staff> handles all role types.
 *  - Inheritance:     Staff subclasses (Teacher, Nurse, etc.) polymorphically loaded.
 *  - Multithreading:  java.awt.Desktop.open() and SMTP run on background Tasks.
 *  - Socket:          NotificationClient listens for real-time application alerts.
 *  - JDBC:            All data persisted via SQLite PreparedStatements.
 *  - File I/O:        HR opens local CV files via java.awt.Desktop.
 */
public class HRDashboardController implements Initializable {

    // ── Staff Tab ─────────────────────────────────────────────────────────────
    @FXML private TableView<Staff>                  staffTable;
    @FXML private TableColumn<Staff, String>        colStaffName;
    @FXML private TableColumn<Staff, String>        colStaffEmail;
    @FXML private TableColumn<Staff, String>        colStaffRole;
    @FXML private TableColumn<Staff, String>        colStaffStatus;
    @FXML private TableColumn<Staff, String>        colStaffCV;

    // ── Student Tab ───────────────────────────────────────────────────────────
    @FXML private TableView<AdmissionApplication>              studentTable;
    @FXML private TableColumn<AdmissionApplication, Integer>   colAppId;
    @FXML private TableColumn<AdmissionApplication, String>    colAppStatus;
    @FXML private TableColumn<AdmissionApplication, String>    colAppDate;
    @FXML private Label selectedStudentInfo;

    // ── Shared UI ─────────────────────────────────────────────────────────────
    @FXML private Label             welcomeLabel;
    @FXML private Label             pendingStaffLabel;
    @FXML private Label             approvedStaffLabel;
    @FXML private Label             studentAppsLabel;
    @FXML private Label             statusLabel;
    @FXML private VBox              notifBox;
    @FXML private HBox              spinnerRow;
    @FXML private ProgressIndicator emailSpinner;

    // ── Services ──────────────────────────────────────────────────────────────
    @SuppressWarnings("rawtypes")
    private final StaffRepository   staffRepo    = new StaffRepository<>();
    private final AdmissionDAO      admissionDAO = new AdmissionDAO();
    private final UserDAO           userDAO      = new UserDAO();
    private final EmailService      emailService = new EmailService();
    private final NotificationClient notifClient  = new NotificationClient();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupStaffTable();
        setupStudentTable();
        refreshAll();
        startNotificationListener();
        if (spinnerRow != null) { spinnerRow.setVisible(false); spinnerRow.setManaged(false); }

        User u = MainApp.getCurrentUser();
        if (u != null) welcomeLabel.setText("🏢  HR Dashboard — " + u.getFullName());
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    private void setupStaffTable() {
        colStaffName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStaffEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStaffRole.setCellValueFactory(new PropertyValueFactory<>("roleType"));
        colStaffStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStaffCV.setCellValueFactory(new PropertyValueFactory<>("cvPath"));

        // Colour-code status column
        colStaffStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "APPROVED" -> "-fx-text-fill: #4CD97B; -fx-font-weight: bold;";
                    case "REJECTED" -> "-fx-text-fill: #FF6B6B; -fx-font-weight: bold;";
                    default         -> "-fx-text-fill: #F5A623;";
                });
            }
        });
    }

    private void setupStudentTable() {
        colAppId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAppStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAppDate.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));

        studentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                if (selected != null)
                    selectedStudentInfo.setText("App #" + selected.getId()
                        + "  |  Status: " + selected.getStatus()
                        + "  |  Payment: " + (selected.isPaymentCompleted() ? "✅ Paid" : "⏳ Pending"));
            }
        );
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    @FXML
    @SuppressWarnings("unchecked")
    public void refreshAll() {
        // Staff
        Response<List<Staff>> sRes = staffRepo.findAll();
        if (sRes.isSuccess()) {
            staffTable.setItems(FXCollections.observableArrayList(sRes.getData()));
            long pending  = sRes.getData().stream().filter(s -> "PENDING".equals(s.getStatus())).count();
            long approved = sRes.getData().stream().filter(s -> "APPROVED".equals(s.getStatus())).count();
            pendingStaffLabel.setText(String.valueOf(pending));
            approvedStaffLabel.setText(String.valueOf(approved));
        }

        // Students
        Response<List<AdmissionApplication>> aRes = admissionDAO.findAll();
        if (aRes.isSuccess()) {
            studentTable.setItems(FXCollections.observableArrayList(aRes.getData()));
            studentAppsLabel.setText(String.valueOf(aRes.getData().size()));
        }
    }

    // ── File I/O: Open CV (background thread) ────────────────────────────────

    @FXML
    private void handleOpenCV() {
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠ Select a staff application first.", false);
            return;
        }
        String path = selected.getCvPath();
        if (path == null || path.isBlank()) {
            setStatus("⚠ No CV path on file for this applicant.", false);
            return;
        }

        // File I/O on background thread — never block the UI
        Task<Void> fileTask = new Task<>() {
            @Override protected Void call() throws Exception {
                File file = new File(path);
                if (!file.exists()) throw new Exception("File not found: " + path);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    throw new Exception("Desktop API not supported on this system.");
                }
                return null;
            }
        };
        fileTask.setOnSucceeded(e -> setStatus("✅ Opened: " + path, true));
        fileTask.setOnFailed(e -> setStatus("❌ " + fileTask.getException().getMessage(), false));

        Thread t = new Thread(fileTask, "CVOpener-Thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Staff Approve & Email ─────────────────────────────────────────────────

    @FXML
    @SuppressWarnings("unchecked")
    private void handleApproveStaff() {
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select a staff application first.", false); return; }
        if ("APPROVED".equals(selected.getStatus())) { setStatus("⚠ Already approved.", false); return; }

        staffRepo.updateStatus(selected.getId(), "APPROVED");

        // Create pending login account for the approved staff member
        userDAO.createPendingUser(selected.getRoleType(), selected.getEmail(), selected.getFullName());

        showSpinner(true);
        setStatus("Sending welcome email to " + selected.getEmail() + "...", true);

        Task<Void> emailTask = new Task<>() {
            @Override protected Void call() throws Exception {
                emailService.sendTeacherWelcomeEmail(selected.getEmail(), selected.getFullName());
                return null;
            }
        };
        emailTask.setOnSucceeded(e -> Platform.runLater(() -> {
            showSpinner(false);
            setStatus("✅ " + selected.getFullName() + " approved! Welcome email sent.", true);
            refreshAll();
        }));
        emailTask.setOnFailed(e -> Platform.runLater(() -> {
            showSpinner(false);
            setStatus("⚠ Approved (DB) — email failed: " + emailTask.getException().getMessage(), false);
            refreshAll();
        }));

        Thread t = new Thread(emailTask, "HREmail-Thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void handleRejectStaff() {
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select a staff application to reject.", false); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Reject application from " + selected.getFullName() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Rejection");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                staffRepo.updateStatus(selected.getId(), "REJECTED");
                setStatus("❌ " + selected.getFullName() + " application rejected.", false);
                refreshAll();
            }
        });
    }

    // ── Student Approve / Reject ──────────────────────────────────────────────

    @FXML
    private void handleApproveStudent() {
        AdmissionApplication sel = studentTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("⚠ Select a student application.", false); return; }
        
        int appId = sel.getId();
        String studentEmail = null;
        String parentEmail = null;
        int studentId = -1;

        // 1. Explicitly update DB to set APPROVED and needs_password_setup = 1
        //    Also fetch the student and parent emails.
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            // Update applications
            PreparedStatement psApp = conn.prepareStatement(
                "UPDATE applications SET status = 'APPROVED', needs_password_setup = 1 WHERE id = ?"
            );
            psApp.setInt(1, appId);
            psApp.executeUpdate();

            // Fetch emails
            PreparedStatement psEmails = conn.prepareStatement(
                "SELECT s.id, s.student_email, p.email AS parent_email " +
                "FROM students s " +
                "JOIN parents p ON s.application_id = p.application_id " +
                "WHERE s.application_id = ?"
            );
            psEmails.setInt(1, appId);
            ResultSet rs = psEmails.executeQuery();
            if (rs.next()) {
                studentId = rs.getInt("id");
                studentEmail = rs.getString("student_email");
                parentEmail = rs.getString("parent_email");
            }
            
            conn.commit();
        } catch (Exception e) {
            setStatus("❌ Database error: " + e.getMessage(), false);
            return;
        }

        sel.setStatus(ApplicationStatus.APPROVED);
        
        if (studentEmail == null || parentEmail == null) {
            setStatus("⚠ Approved, but emails not found for Student ID " + studentId, false);
            refreshAll();
            return;
        }

        // Create pending user for the student
        userDAO.createPendingUser("STUDENT", studentEmail, "Student " + studentId);
        
        // 2. Dual-Email Thread
        showSpinner(true);
        setStatus("Sending activation emails...", true);

        final String finalSEmail = studentEmail;
        final String finalPEmail = parentEmail;
        final int finalSId = studentId;

        Task<Void> emailTask = new Task<>() {
            @Override protected Void call() throws Exception {
                emailService.sendActivationEmails(finalSEmail, finalPEmail, finalSId);
                return null;
            }
        };
        
        emailTask.setOnSucceeded(e -> Platform.runLater(() -> {
            showSpinner(false);
            setStatus("✅ Student Application #" + appId + " approved! Emails sent.", true);
            refreshAll();
        }));
        
        emailTask.setOnFailed(e -> Platform.runLater(() -> {
            showSpinner(false);
            // Silent Failure fix: Extract cause message to UI
            Throwable ex = emailTask.getException();
            setStatus("⚠ Approved (DB) — email failed: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
            refreshAll();
        }));

        Thread t = new Thread(emailTask, "StudentApproveEmail-Thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleRejectStudent() {
        AdmissionApplication sel = studentTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("⚠ Select a student application.", false); return; }
        sel.setStatus(ApplicationStatus.REJECTED);
        admissionDAO.update(sel);
        setStatus("❌ Student Application #" + sel.getId() + " rejected.", false);
        refreshAll();
    }

    // ── Socket Listener ───────────────────────────────────────────────────────

    private void startNotificationListener() {
        notifClient.connect(msg -> {
            Label lbl = new Label("🔔 " + msg);
            lbl.setStyle("-fx-text-fill: #C9A84C; -fx-font-size:12px; -fx-padding: 4 8;");
            lbl.setWrapText(true);

            FadeTransition ft = new FadeTransition(Duration.millis(400), lbl);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            notifBox.getChildren().add(0, lbl);
            if (notifBox.getChildren().size() > 10) notifBox.getChildren().remove(10);

            // Auto-refresh table when a new application comes in
            refreshAll();
        });
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void setStatus(String msg, boolean success) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle(success ? "-fx-text-fill:#4CD97B;" : "-fx-text-fill:#FF6B6B;");
        });
    }

    private void showSpinner(boolean show) {
        Platform.runLater(() -> {
            if (spinnerRow != null) { spinnerRow.setVisible(show); spinnerRow.setManaged(show); }
        });
    }

    @FXML
    private void logout() {
        notifClient.disconnect();
        MainApp.logout();
    }
}
