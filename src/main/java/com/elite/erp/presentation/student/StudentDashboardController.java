package com.elite.erp.presentation.student;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.User;
import com.elite.erp.model.Grade;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import com.elite.erp.presentation.announcements.AnnouncementFeedWidget;

public class StudentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> calendarListView;
    @FXML private VBox announcementsContainer;
    @FXML private ListView<MaterialItem> materialsListView;
    @FXML private ListView<ExamItem> examsListView;

    @FXML private TextArea chatArea;
    @FXML private TextField chatInputField;

    // Timetable weekly grid
    @FXML private Tab timetableTab;
    @FXML private VBox monBox;
    @FXML private VBox tueBox;
    @FXML private VBox wedBox;
    @FXML private VBox thuBox;
    @FXML private Label timetableHint;

    @FXML private TabPane studentTabPane;

    // Grades
    @FXML private Tab gradesTab;
    @FXML private TableView<Grade> gradesTable;
    @FXML private TableColumn<Grade, String> colGradeDate;
    @FXML private TableColumn<Grade, String> colGradeSubject;
    @FXML private TableColumn<Grade, Number> colGradeScore;

    private User currentUser;
    private int studentClassId = -1;
    private int studentRecordId = -1;

    private Socket chatSocket;
    private PrintWriter chatWriter;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public static class MaterialItem {
        public int id; public String title; public String path;
        public MaterialItem(int id, String title, String path) { this.id=id; this.title=title; this.path=path; }
        @Override public String toString() { return title; }
    }

    public static class ExamItem {
        public int id; public String title; public int duration;
        public ExamItem(int id, String title, int duration) { this.id=id; this.title=title; this.duration=duration; }
        @Override public String toString() { return title + " (" + duration + " mins)"; }
    }

    // ── Initialization ────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = MainApp.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFullName() + "!");
            loadStudentClassId();
            loadCalendar();

            if (announcementsContainer != null) {
                announcementsContainer.getChildren().add(new AnnouncementFeedWidget("STUDENT"));
            }

            if (studentClassId > 0) {
                loadClassroomData();
                loadTimetable();
                connectToChat();
            } else {
                if (chatArea != null) chatArea.appendText("[System] You are not assigned to a class yet. Contact admin.\n");
                if (timetableHint != null) timetableHint.setText("Not assigned to a class yet — contact your admin.");
            }
            if (gradesTable != null) setupGradesTable();
        }
    }

    private void loadStudentClassId() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Try by student_email (primary path)
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, class_id FROM students WHERE student_email = ?");
            ps.setString(1, currentUser.getEmail());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                studentRecordId = rs.getInt("id");
                int cid = rs.getInt("class_id");
                // SQLite returns 0 for NULL integers — treat 0 as unassigned
                studentClassId = (cid > 0) ? cid : -1;
            }
            // Fallback: match by user full_name (if email differs between tables)
            if (studentRecordId == -1) {
                PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT id, class_id FROM students WHERE first_name || ' ' || last_name = ?");
                ps2.setString(1, currentUser.getFullName());
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    studentRecordId = rs2.getInt("id");
                    int cid = rs2.getInt("class_id");
                    if (cid > 0) studentClassId = cid;
                }
            }
            System.out.println("[Student] classId resolved to: " + studentClassId
                + " for email=" + currentUser.getEmail());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadCalendar() {
        calendarListView.getItems().addAll(
            "Jan 7  - Coptic Christmas Day",
            "Jan 25 - Revolution Day",
            "Mar 30 - Eid Al-Fitr (Approx)",
            "Apr 21 - Sham El-Nessim",
            "Apr 25 - Sinai Liberation Day",
            "Jun 6  - Eid Al-Adha (Approx)",
            "Jun 26 - Islamic New Year",
            "Oct 6  - Armed Forces Day"
        );
    }

    // ── Classroom ─────────────────────────────────────────────────────────────

    private void loadClassroomData() {
        materialsListView.getItems().clear();
        examsListView.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement matPs = conn.prepareStatement(
                "SELECT id, title, file_path FROM assignments WHERE class_id = ?");
            matPs.setInt(1, studentClassId);
            ResultSet matRs = matPs.executeQuery();
            while (matRs.next()) {
                materialsListView.getItems().add(new MaterialItem(
                    matRs.getInt("id"), matRs.getString("title"), matRs.getString("file_path")));
            }

            // Show exams that are still active (not expired) OR have no expiry set (backward compat)
            PreparedStatement examPs = conn.prepareStatement(
                "SELECT id, title, duration_minutes FROM exams " +
                "WHERE class_id = ? AND (" +
                "  expiry_timestamp IS NULL" +
                "  OR expiry_timestamp = ''" +
                "  OR expiry_timestamp > strftime('%Y-%m-%d %H:%M:%S','now','localtime')" +
                ")");
            examPs.setInt(1, studentClassId);
            ResultSet examRs = examPs.executeQuery();
            while (examRs.next()) {
                examsListView.getItems().add(new ExamItem(
                    examRs.getInt("id"), examRs.getString("title"), examRs.getInt("duration_minutes")));
            }
            System.out.println("[Student] loaded " + examsListView.getItems().size()
                + " exam(s) for class_id=" + studentClassId);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void openMaterial() {
        MaterialItem item = materialsListView.getSelectionModel().getSelectedItem();
        if (item != null) {
            try {
                File file = new File(item.path);
                if (file.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
                else showAlert("File Not Found", "Could not open: " + item.path);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML private void startExam() {
        ExamItem item = examsListView.getSelectionModel().getSelectedItem();
        if (item != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/elite/erp/fxml/LockdownExam.fxml"));
                Parent root = loader.load();
                LockdownExamController controller = loader.getController();
                controller.initExam(item.id, item.title, item.duration);
                Stage examStage = new Stage();
                examStage.setScene(new Scene(root));
                welcomeLabel.getScene().getWindow().hide();
                examStage.show();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ── Timetable ─────────────────────────────────────────────────────────────

    @FXML private void showTimetable() {
        if (studentTabPane != null && timetableTab != null)
            studentTabPane.getSelectionModel().select(timetableTab);
    }

    @FXML private void showGrades() {
        if (studentTabPane != null && gradesTab != null) {
            studentTabPane.getSelectionModel().select(gradesTab);
            loadGrades();
        }
    }

    private void setupGradesTable() {
        colGradeDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));
        colGradeSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        colGradeScore.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getScore()));
        loadGrades();
    }

    private void loadGrades() {
        if (gradesTable == null || studentRecordId == -1) return;
        gradesTable.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM grades WHERE student_id = ? ORDER BY date DESC");
            ps.setInt(1, studentRecordId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                gradesTable.getItems().add(new Grade(
                    rs.getInt("id"), rs.getInt("student_id"),
                    rs.getString("subject"), rs.getDouble("score"), rs.getString("date")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTimetable() {
        if (monBox == null) return;
        monBox.getChildren().clear();
        tueBox.getChildren().clear();
        wedBox.getChildren().clear();
        thuBox.getChildren().clear();

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT subject, subject_type, day_1, day_2, day_3, duration_minutes, " +
                "COALESCE(start_time, '08:30') AS start_time " +
                "FROM class_schedule WHERE class_id = ? ORDER BY start_time, subject"
            );
            ps.setInt(1, studentClassId);
            ResultSet rs = ps.executeQuery();

            boolean hasAny = false;
            while (rs.next()) {
                String subject   = rs.getString("subject");
                String type      = rs.getString("subject_type");
                int    duration  = rs.getInt("duration_minutes");
                String startTime = rs.getString("start_time");
                List<String> days = new ArrayList<>();
                for (String col : new String[]{"day_1","day_2","day_3"}) {
                    String d = rs.getString(col);
                    if (d != null && !d.isBlank()) days.add(d);
                }
                for (String day : days) {
                    VBox box = getDayBox(day);
                    if (box != null) { box.getChildren().add(buildSubjectCard(subject, type, startTime, duration)); hasAny = true; }
                }
            }
            addPlaceholderIfEmpty(monBox);
            addPlaceholderIfEmpty(tueBox);
            addPlaceholderIfEmpty(wedBox);
            addPlaceholderIfEmpty(thuBox);
            if (timetableHint != null)
                timetableHint.setText(hasAny ? "" : "No timetable set yet — check back later.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox getDayBox(String day) {
        if (day == null) return null;
        return switch (day.trim().toLowerCase()) {
            case "monday"    -> monBox;
            case "tuesday"   -> tueBox;
            case "wednesday" -> wedBox;
            case "thursday"  -> thuBox;
            default          -> null;
        };
    }

    private VBox buildSubjectCard(String subject, String type, String startTime, int duration) {
        VBox card = new VBox(4);
        boolean isCore = "CORE".equalsIgnoreCase(type);
        String bg    = isCore ? "#1E3A5F" : "#2D1B4E";
        String badge = isCore ? "#C9A84C" : "#9B59B6";
        card.setStyle("-fx-background-color: " + bg + "; -fx-padding: 10 12; -fx-background-radius: 8; " +
                      "-fx-border-color: " + badge + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 8 8 0;");
        Label sub = new Label(subject);
        sub.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        sub.setWrapText(true);
        String timeStr = (startTime != null && !startTime.isBlank()) ? startTime : "";
        Label info = new Label((isCore ? "📘 Core" : "🎨 Minor")
            + (timeStr.isBlank() ? "" : "  🕗 " + timeStr)
            + "  · " + duration + " min");
        info.setStyle("-fx-text-fill: " + badge + "; -fx-font-size: 11px;");
        card.getChildren().addAll(sub, info);
        return card;
    }

    private void addPlaceholderIfEmpty(VBox box) {
        if (box != null && box.getChildren().isEmpty()) {
            Label ph = new Label("Free day");
            ph.setStyle("-fx-text-fill: #4A5568; -fx-font-size: 11px; -fx-font-style: italic;");
            box.getChildren().add(ph);
        }
    }

    // ── Group Chat ────────────────────────────────────────────────────────────

    private void connectToChat() {
        new Thread(() -> {
            try {
                chatSocket = new Socket("localhost", 9998);
                chatWriter = new PrintWriter(new OutputStreamWriter(chatSocket.getOutputStream()), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));

                // Send JOIN with role=STUDENT
                chatWriter.println("JOIN|" + studentClassId + "|" + currentUser.getFullName()
                        + "|" + currentUser.getId() + "|STUDENT");

                // ── Load chat history from DB first ──
                loadChatHistory();

                // Then listen for live messages
                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (chatArea != null) chatArea.appendText("[System] Disconnected from chat server.\n");
                });
            }
        }).start();
    }

    private void loadChatHistory() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT sender_name, message, timestamp FROM chat_history " +
                "WHERE class_id = ? ORDER BY id DESC LIMIT 50");
            ps.setInt(1, studentClassId);
            ResultSet rs = ps.executeQuery();

            // Collect in reverse so oldest message is first
            List<String> history = new ArrayList<>();
            while (rs.next()) {
                String sender = rs.getString("sender_name");
                String msg    = rs.getString("message");
                String ts     = rs.getString("timestamp");
                history.add(0, "[" + ts.substring(11, 16) + "] " + sender + ": " + msg);
            }

            if (!history.isEmpty()) {
                Platform.runLater(() -> {
                    chatArea.appendText("── Chat History ────────────────────\n");
                    history.forEach(m -> chatArea.appendText(m + "\n"));
                    chatArea.appendText("── Live ────────────────────────────\n");
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void sendChatMessage() {
        String msg = chatInputField.getText().trim();
        if (!msg.isEmpty() && chatWriter != null) {
            chatWriter.println("MSG|" + currentUser.getFullName() + "|" + msg);
            chatInputField.clear();
        }
    }

    @FXML private void deleteChatMessage() {
        // Delete the last message this student sent from DB
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM chat_history WHERE id = (" +
                "SELECT id FROM chat_history WHERE class_id = ? AND sender_name = ? ORDER BY id DESC LIMIT 1)");
            ps.setInt(1, studentClassId);
            ps.setString(2, currentUser.getFullName());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                Platform.runLater(() -> {
                    chatArea.appendText("[System] Your last message was deleted.\n");
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void uploadChatMedia() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Share a File in Chat");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(chatInputField.getScene().getWindow());
        if (file != null && chatWriter != null) {
            String shareMsg = "📎 Shared file: " + file.getName() + "  [Path: " + file.getAbsolutePath() + "]";
            chatWriter.println("MSG|" + currentUser.getFullName() + "|" + shareMsg);
        }
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    @FXML private void openAnnouncements() {
        MainApp.switchScene("/com/elite/erp/fxml/AnnouncementsFeed.fxml");
    }

    @FXML private void logout() {
        if (chatSocket != null && !chatSocket.isClosed()) {
            try { chatSocket.close(); } catch (Exception ignored) {}
        }
        MainApp.setCurrentUser(null);
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}
