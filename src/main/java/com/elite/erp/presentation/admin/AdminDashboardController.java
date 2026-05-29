package com.elite.erp.presentation.admin;

import com.elite.erp.MainApp;
import com.elite.erp.dao.AdmissionDAO;
import com.elite.erp.model.AdmissionApplication;
import com.elite.erp.model.ApplicationStatus;
import com.elite.erp.network.NotificationClient;
import com.elite.erp.util.Response;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import com.elite.erp.dao.DatabaseManager;

/**
 * AdminDashboardController — System Admin.
 * Manages overview stats, class assignments, and class timetable.
 */
public class AdminDashboardController implements Initializable {

    // ── Overview tab ─────────────────────────────────────────────────────────
    @FXML private Label totalPendingLabel;
    @FXML private Label totalApprovedLabel;
    @FXML private Label totalRejectedLabel;
    @FXML private TableView<AdmissionApplication> applicationsTable;
    @FXML private TableColumn<AdmissionApplication, Integer> colAppId;
    @FXML private TableColumn<AdmissionApplication, String>  colStatus;
    @FXML private TableColumn<AdmissionApplication, String>  colDate;
    @FXML private VBox   notifBox;
    @FXML private Label  actionStatusLabel;

    // ── Class Assignment tab ──────────────────────────────────────────────────
    @FXML private Label              assignmentStatus;
    @FXML private ComboBox<ComboItem> teacherCombo;
    @FXML private VBox               teacherClassChecksBox;
    @FXML private ComboBox<ComboItem> studentCombo;
    @FXML private ComboBox<ComboItem> studentClassCombo;

    // ── Timetable tab ─────────────────────────────────────────────────────────
    @FXML private Label                          scheduleStatus;
    @FXML private ComboBox<ComboItem>            scheduleClassCombo;
    @FXML private TextField                      subjectField;
    @FXML private RadioButton                    rbCore;
    @FXML private RadioButton                    rbMinor;
    @FXML private CheckBox                       cbMon;
    @FXML private CheckBox                       cbTue;
    @FXML private CheckBox                       cbWed;
    @FXML private CheckBox                       cbThu;
    @FXML private CheckBox                       cbFri;
    @FXML private CheckBox                       cbSat;
    @FXML private TextField                      durationField;
    @FXML private Label                          durationHintLabel;
    @FXML private ComboBox<String>               startTimeCombo;
    @FXML private TableView<ScheduleEntry>       scheduleTable;
    @FXML private TableColumn<ScheduleEntry, String> colSchedClass;
    @FXML private TableColumn<ScheduleEntry, String> colSchedSubject;
    @FXML private TableColumn<ScheduleEntry, String> colSchedType;
    @FXML private TableColumn<ScheduleEntry, String> colSchedTime;
    @FXML private TableColumn<ScheduleEntry, String> colSchedDays;
    @FXML private TableColumn<ScheduleEntry, String> colSchedDur;

    private final AdmissionDAO       admissionDAO = new AdmissionDAO();
    private final NotificationClient notifClient  = new NotificationClient();

    // ── School timetable rules ────────────────────────────────────────────────
    // School: 08:30 – 14:30  |  Lunch: 12:30 (fixed break)  |  Dismissal: 15:00
    private static final String[] MORNING_SLOTS   = {
        "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00"
    };
    private static final String[] AFTERNOON_SLOTS = {
        "13:30", "14:00", "14:30"   // lunch ends ~13:30, last period ends by 14:30
    };
    // 12:30 = lunch break (blocked), 15:00 = dismissal (blocked)

    // ── Schedule DTO ──────────────────────────────────────────────────────────
    public static class ScheduleEntry {
        public int    id;
        public String className, subject, subjectType, days, startTime;
        public int    durationMinutes;

        public ScheduleEntry(int id, String className, String subject,
                             String subjectType, String days,
                             String startTime, int durationMinutes) {
            this.id = id; this.className = className; this.subject = subject;
            this.subjectType = subjectType; this.days = days;
            this.startTime = (startTime != null ? startTime : "08:30");
            this.durationMinutes = durationMinutes;
        }

        public String getClassName()    { return className; }
        public String getSubject()      { return subject; }
        public String getSubjectType()  { return subjectType; }
        public String getStartTime()    { return startTime; }
        public String getDays()         { return days; }
        public String getDurStr()       { return durationMinutes + " min"; }
    }

    // ── ComboItem ─────────────────────────────────────────────────────────────
    public static class ComboItem {
        public final int id; public final String display;
        public ComboItem(int id, String display) { this.id = id; this.display = display; }
        @Override public String toString() { return display; }
    }

    // ── Initialization ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            setupTable();
            loadApplications();
            startNotificationListener();
            if (actionStatusLabel != null) {
                actionStatusLabel.setText("ℹ Application approvals are handled by HR (HR111@gmail.com).");
                actionStatusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size:11px;");
            }
            loadAssignmentData();
            setupScheduleTable();
            populateTimeCombo();
        } catch (Exception e) {
            System.err.println("[AdminController] Init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Populate valid school time slots ──────────────────────────────────────
    private void populateTimeCombo() {
        if (startTimeCombo == null) return;
        startTimeCombo.getItems().clear();

        // ── Morning session: 08:30 – 12:00 ──
        startTimeCombo.getItems().add("── Morning ──");
        for (String t : MORNING_SLOTS) startTimeCombo.getItems().add(t);

        // ── Lunch 12:30 — blocked (shown as info but not selectable via validation) ──
        startTimeCombo.getItems().add("── 12:30 LUNCH BREAK ──");

        // ── Afternoon session: 13:30 – 14:30 ──
        startTimeCombo.getItems().add("── Afternoon ──");
        for (String t : AFTERNOON_SLOTS) startTimeCombo.getItems().add(t);

        startTimeCombo.getSelectionModel().select("08:30");
    }

    // ── Applications overview ─────────────────────────────────────────────────
    private void setupTable() {
        colAppId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));
    }

    @FXML public void refreshTable() { loadApplications(); }

    private void loadApplications() {
        Response<List<AdmissionApplication>> res = admissionDAO.findAll();
        if (res.isSuccess()) {
            applicationsTable.getItems().setAll(res.getData());
            refreshStats(res.getData());
        }
    }

    private void refreshStats(List<AdmissionApplication> apps) {
        long pending  = apps.stream().filter(a -> a.getStatus() == ApplicationStatus.PENDING).count();
        long approved = apps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
        long rejected = apps.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count();
        totalPendingLabel.setText(String.valueOf(pending));
        totalApprovedLabel.setText(String.valueOf(approved));
        totalRejectedLabel.setText(String.valueOf(rejected));
    }

    @FXML private void openAnnouncements() {
        MainApp.switchScene("/com/elite/erp/fxml/AnnouncementsFeed.fxml");
    }

    // ── Socket notification listener ─────────────────────────────────────────
    private void startNotificationListener() {
        notifClient.connect(msg -> {
            Label lbl = new Label("🔔 " + msg);
            lbl.setStyle("-fx-text-fill: #C9A84C; -fx-font-size:12px; -fx-padding: 4 8;");
            lbl.setWrapText(true);
            FadeTransition ft = new FadeTransition(Duration.millis(400), lbl);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            notifBox.getChildren().add(0, lbl);
            if (notifBox.getChildren().size() > 8)
                notifBox.getChildren().remove(8);
            loadApplications();
        });
    }

    // ── Class Assignment ──────────────────────────────────────────────────────
    private void loadAssignmentData() {
        if (teacherCombo == null) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pt = conn.prepareStatement(
                "SELECT id, full_name FROM users WHERE role='TEACHER'");
            ResultSet rt = pt.executeQuery();
            while (rt.next())
                teacherCombo.getItems().add(new ComboItem(rt.getInt("id"), rt.getString("full_name")));

            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, first_name, last_name FROM students");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                studentCombo.getItems().add(new ComboItem(rs.getInt("id"),
                    rs.getString("first_name") + " " + rs.getString("last_name")));

            PreparedStatement pc = conn.prepareStatement("SELECT id, name FROM classes");
            ResultSet rc = pc.executeQuery();
            while (rc.next()) {
                int    cid   = rc.getInt("id");
                String cname = rc.getString("name");
                studentClassCombo.getItems().add(new ComboItem(cid, cname));
                if (scheduleClassCombo != null)
                    scheduleClassCombo.getItems().add(new ComboItem(cid, cname));

                CheckBox cb = new CheckBox(cname);
                cb.setUserData(cid);
                cb.setStyle("-fx-text-fill: white; -fx-padding: 2;");
                teacherClassChecksBox.getChildren().add(cb);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void saveTeacherAssignment() {
        ComboItem teacher = teacherCombo.getValue();
        if (teacher == null) {
            assignmentStatus.setText("❌ Please select a teacher.");
            assignmentStatus.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement del = conn.prepareStatement(
                "DELETE FROM teacher_classes WHERE user_id = ?");
            del.setInt(1, teacher.id); del.executeUpdate();

            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO teacher_classes (user_id, class_id) VALUES (?, ?)");
            for (var node : teacherClassChecksBox.getChildren()) {
                if (node instanceof CheckBox cb && cb.isSelected()) {
                    ins.setInt(1, teacher.id); ins.setInt(2, (Integer) cb.getUserData());
                    ins.addBatch();
                }
            }
            ins.executeBatch();
            assignmentStatus.setText("✅ Teacher classes updated.");
            assignmentStatus.setStyle("-fx-text-fill: #4CD97B;");
        } catch (Exception e) {
            e.printStackTrace();
            assignmentStatus.setText("❌ DB error: " + e.getMessage());
            assignmentStatus.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    @FXML private void saveStudentAssignment() {
        ComboItem student = studentCombo.getValue();
        ComboItem cls     = studentClassCombo.getValue();
        if (student == null || cls == null) {
            assignmentStatus.setText("❌ Please select student and class.");
            assignmentStatus.setStyle("-fx-text-fill: #FF6B6B;");
            return;
        }
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pt = conn.prepareStatement(
                "UPDATE students SET class_id = ? WHERE id = ?");
            pt.setInt(1, cls.id); pt.setInt(2, student.id);
            pt.executeUpdate();
            assignmentStatus.setText("✅ Student assigned to " + cls.display + ".");
            assignmentStatus.setStyle("-fx-text-fill: #4CD97B;");
        } catch (Exception e) {
            e.printStackTrace();
            assignmentStatus.setText("❌ DB error: " + e.getMessage());
            assignmentStatus.setStyle("-fx-text-fill: #FF6B6B;");
        }
    }

    // ── Timetable ─────────────────────────────────────────────────────────────
    private void setupScheduleTable() {
        if (scheduleTable == null) return;
        colSchedClass.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue().getClassName()));
        colSchedSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        colSchedType.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getSubjectType()));
        colSchedTime.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getStartTime()));
        colSchedDays.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getDays()));
        colSchedDur.setCellValueFactory(d     -> new SimpleStringProperty(d.getValue().getDurStr()));
        loadAllSchedules();
    }

    @FXML private void onSubjectTypeChanged() {
        if (durationHintLabel == null) return;
        boolean isCore = rbCore != null && rbCore.isSelected();
        durationHintLabel.setText(isCore
            ? "Core: max 120 min  |  up to 3 days / week"
            : "Minor: max 60 min  |  up to 2 days / week");
    }

    @FXML private void onScheduleClassSelected() { loadAllSchedules(); }

    @FXML public void loadAllSchedules() {
        if (scheduleTable == null) return;
        scheduleTable.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT cs.id, c.name, cs.subject, cs.subject_type, " +
                "cs.day_1, cs.day_2, cs.day_3, cs.duration_minutes, cs.start_time " +
                "FROM class_schedule cs " +
                "JOIN classes c ON c.id = cs.class_id " +
                "ORDER BY c.name, cs.start_time, cs.subject"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                List<String> days = new ArrayList<>();
                for (String col : new String[]{"day_1","day_2","day_3"}) {
                    String d = rs.getString(col);
                    if (d != null && !d.isBlank()) days.add(d);
                }
                scheduleTable.getItems().add(new ScheduleEntry(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("subject"),
                    rs.getString("subject_type"),
                    String.join(", ", days),
                    rs.getString("start_time"),
                    rs.getInt("duration_minutes")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void saveScheduleEntry() {
        if (scheduleClassCombo == null) return;

        ComboItem cls     = scheduleClassCombo.getValue();
        String  subject   = subjectField   != null ? subjectField.getText().trim()   : "";
        boolean isCore    = rbCore         != null && rbCore.isSelected();
        String  type      = isCore ? "CORE" : "MINOR";
        int     maxDays   = isCore ? 3 : 2;
        int     maxDur    = isCore ? 120 : 60;
        String  startTime = startTimeCombo != null ? startTimeCombo.getValue() : null;

        // ── Validation ────────────────────────────────────────────────────────
        if (cls == null)        { setScheduleStatus("❌ Please select a class.", false); return; }
        if (subject.isEmpty())  { setScheduleStatus("❌ Enter a subject name.", false);  return; }

        // Block header/separator rows (not real times)
        if (startTime == null || startTime.startsWith("──")) {
            setScheduleStatus("❌ Pick a valid start time (not the header row).", false);
            return;
        }
        // Block lunch break 12:30
        if ("12:30".equals(startTime)) {
            setScheduleStatus("❌ 12:30 is the lunch break — cannot schedule here.", false);
            return;
        }

        // Collect days
        List<String> selectedDays = new ArrayList<>();
        if (cbMon != null && cbMon.isSelected()) selectedDays.add("Monday");
        if (cbTue != null && cbTue.isSelected()) selectedDays.add("Tuesday");
        if (cbWed != null && cbWed.isSelected()) selectedDays.add("Wednesday");
        if (cbThu != null && cbThu.isSelected()) selectedDays.add("Thursday");

        if (selectedDays.isEmpty())              { setScheduleStatus("❌ Select at least one day.", false); return; }
        if (selectedDays.size() > maxDays)       { setScheduleStatus("❌ " + type + " allows max " + maxDays + " days/week.", false); return; }

        int duration;
        try { duration = Integer.parseInt(durationField != null ? durationField.getText().trim() : "0"); }
        catch (NumberFormatException e) { setScheduleStatus("❌ Duration must be a number.", false); return; }
        if (duration <= 0 || duration > maxDur)  { setScheduleStatus("❌ Duration must be 1–" + maxDur + " min for " + type + " subjects.", false); return; }

        // ── Check period doesn't overrun lunch or dismissal ───────────────────
        String endTimeErr = checkPeriodOverrun(startTime, duration);
        if (endTimeErr != null) { setScheduleStatus(endTimeErr, false); return; }

        String d1 = selectedDays.size() > 0 ? selectedDays.get(0) : null;
        String d2 = selectedDays.size() > 1 ? selectedDays.get(1) : null;
        String d3 = selectedDays.size() > 2 ? selectedDays.get(2) : null;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO class_schedule (class_id, subject, subject_type, " +
                "day_1, day_2, day_3, duration_minutes, start_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setInt(1, cls.id);     ps.setString(2, subject);
            ps.setString(3, type);    ps.setString(4, d1);
            ps.setString(5, d2);      ps.setString(6, d3);
            ps.setInt(7, duration);   ps.setString(8, startTime);
            ps.executeUpdate();

            setScheduleStatus("✅ Added: " + subject + " @ " + startTime + " [" + type + "] for " + cls.display, true);

            // Reset form
            if (subjectField  != null) subjectField.clear();
            if (durationField != null) durationField.clear();
            if (cbMon != null) cbMon.setSelected(false);
            if (cbTue != null) cbTue.setSelected(false);
            if (cbWed != null) cbWed.setSelected(false);
            if (cbThu != null) cbThu.setSelected(false);
            if (startTimeCombo != null) startTimeCombo.getSelectionModel().select("08:30");
            loadAllSchedules();

        } catch (Exception e) {
            e.printStackTrace();
            setScheduleStatus("❌ DB error: " + e.getMessage(), false);
        }
    }

    /**
     * Verifies that a period starting at startTime with given duration minutes
     * does not run into the lunch break (12:30) or past dismissal (15:00).
     * @return null if OK, error message string if there is a conflict
     */
    private String checkPeriodOverrun(String startTime, int durationMins) {
        try {
            String[] parts = startTime.split(":");
            int startH = Integer.parseInt(parts[0]);
            int startM = Integer.parseInt(parts[1]);
            int startTotal  = startH * 60 + startM;
            int endTotal    = startTotal + durationMins;

            int lunchStart  = 12 * 60 + 30;   // 12:30
            int dismissal   = 15 * 60;         // 15:00

            if (startTotal < lunchStart && endTotal > lunchStart) {
                int overrun = endTotal - lunchStart;
                return "❌ Period runs into lunch break (12:30) by " + overrun + " min. Shorten or move after 13:30.";
            }
            if (endTotal > dismissal) {
                int overrun = endTotal - dismissal;
                return "❌ Period runs past dismissal (15:00) by " + overrun + " min. Shorten or start earlier.";
            }
        } catch (Exception ignored) {}
        return null;
    }

    @FXML private void deleteScheduleEntry() {
        if (scheduleTable == null) return;
        ScheduleEntry sel = scheduleTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setScheduleStatus("❌ Select a row to delete.", false); return; }
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM class_schedule WHERE id = ?");
            ps.setInt(1, sel.id); ps.executeUpdate();
            setScheduleStatus("🗑 Deleted: " + sel.getSubject(), true);
            loadAllSchedules();
        } catch (Exception e) {
            e.printStackTrace();
            setScheduleStatus("❌ Delete failed: " + e.getMessage(), false);
        }
    }

    private void setScheduleStatus(String msg, boolean ok) {
        if (scheduleStatus == null) return;
        scheduleStatus.setText(msg);
        scheduleStatus.setStyle(ok
            ? "-fx-text-fill: #4CD97B; -fx-font-weight: bold;"
            : "-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
    }

    @FXML private void logout() {
        notifClient.disconnect();
        MainApp.switchScene("/com/elite/erp/fxml/Login.fxml");
    }
}
