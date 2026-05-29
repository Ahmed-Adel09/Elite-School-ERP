package com.elite.erp.presentation.clinic;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * StudentClinicController — Read-only medical profile view for the student.
 * Demonstrates: JDBC read, Socket client, and JavaFX TableView.
 */
public class StudentClinicController implements Initializable {

    @FXML private Label bloodTypeLabel;
    @FXML private Label allergiesLabel;
    @FXML private Label emergencyLabel;
    @FXML private Label statusLabel;
    @FXML private Button emergencyBtn;

    @FXML private TableView<VisitRow> visitsTable;
    @FXML private TableColumn<VisitRow, String> colDate;
    @FXML private TableColumn<VisitRow, String> colSymptoms;
    @FXML private TableColumn<VisitRow, String> colTreatment;

    private User currentUser;
    private int studentDbId = -1;   // id from the `students` table (NOT users.id)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = MainApp.getCurrentUser();
        setupTable();
        if (currentUser != null) {
            resolveStudentId();
            loadMedicalProfile();
            loadVisitHistory();
        }
    }

    // ── Table wiring ─────────────────────────────────────────────────────────

    private void setupTable() {
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().dateTime));
        colSymptoms.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symptoms));
        colTreatment.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().treatment));
        visitsTable.setPlaceholder(new Label("No clinic visits on record."));
    }

    // ── Resolve students.id from the logged-in user's email ──────────────────

    private void resolveStudentId() {
        String sql = "SELECT id FROM students WHERE student_email = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUser.getEmail());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) studentDbId = rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("[StudentClinic] resolveStudentId error: " + e.getMessage());
        }
    }

    // ── Load medical profile ──────────────────────────────────────────────────

    private void loadMedicalProfile() {
        if (studentDbId == -1) {
            // Fall back: try pulling allergies directly from the students table
            // (populated during admission wizard Step 3)
            String sql = "SELECT allergies FROM students WHERE student_email = ? LIMIT 1";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUser.getEmail());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        allergiesLabel.setText(nvl(rs.getString("allergies"), "None recorded"));
                    }
                }
            } catch (Exception e) {
                System.err.println("[StudentClinic] fallback profile error: " + e.getMessage());
            }
            bloodTypeLabel.setText("Not on file");
            emergencyLabel.setText("Not on file");
            return;
        }

        String sql = "SELECT blood_type, allergies, emergency_contact FROM medical_profiles WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentDbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bloodTypeLabel.setText(nvl(rs.getString("blood_type"), "Not on file"));
                    allergiesLabel.setText(nvl(rs.getString("allergies"), "None"));
                    emergencyLabel.setText(nvl(rs.getString("emergency_contact"), "Not recorded"));
                } else {
                    // No medical_profile row yet — pull allergies from students table as fallback
                    loadFallbackAllergies();
                }
            }
        } catch (Exception e) {
            System.err.println("[StudentClinic] loadMedicalProfile error: " + e.getMessage());
        }
    }

    private void loadFallbackAllergies() {
        String sql = "SELECT allergies FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentDbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) allergiesLabel.setText(nvl(rs.getString("allergies"), "None recorded"));
            }
        } catch (Exception e) {
            System.err.println("[StudentClinic] fallbackAllergies error: " + e.getMessage());
        }
        bloodTypeLabel.setText("Not on file");
        emergencyLabel.setText("Not on file");
    }

    // ── Load visit history ────────────────────────────────────────────────────

    private void loadVisitHistory() {
        if (studentDbId == -1) return;
        ObservableList<VisitRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT date_time, symptoms, treatment_given FROM clinic_visits WHERE student_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentDbId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new VisitRow(
                        rs.getString("date_time"),
                        nvl(rs.getString("symptoms"), "—"),
                        nvl(rs.getString("treatment_given"), "—")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[StudentClinic] loadVisitHistory error: " + e.getMessage());
        }
        visitsTable.setItems(rows);
    }

    // ── Socket: Request Nurse Assistance (Port 9997) ──────────────────────────

    @FXML
    private void requestNurse() {
        emergencyBtn.setDisable(true);
        setStatus("📡 Sending alert...", "#F5A623");

        String studentName = (currentUser != null) ? currentUser.getFullName() : "Unknown";
        String message = "EMERGENCY|" + studentName + " (ID:" + studentDbId + ") needs urgent nurse assistance!";

        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 9997);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
                out.println(message);
                Platform.runLater(() -> {
                    setStatus("✅ Alert sent! The nurse has been notified.", "#4CD97B");
                    // Re-enable after 30 s to prevent spam
                    new Thread(() -> {
                        try { Thread.sleep(30_000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            emergencyBtn.setDisable(false);
                            setStatus("", "#4CD97B");
                        });
                    }).start();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("❌ Server offline. Please go to the clinic directly.", "#FF6B6B");
                    emergencyBtn.setDisable(false);
                });
            }
        }, "NurseAlert-Thread").start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String msg, String hex) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold;");
    }

    private String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    // ── Inner model ───────────────────────────────────────────────────────────

    public static class VisitRow {
        final String dateTime, symptoms, treatment;
        VisitRow(String dt, String s, String t) { dateTime = dt; symptoms = s; treatment = t; }
    }
}
