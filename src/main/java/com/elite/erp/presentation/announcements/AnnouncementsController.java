package com.elite.erp.presentation.announcements;

import com.elite.erp.MainApp;
import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.Announcement;
import com.elite.erp.model.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AnnouncementsController — Broadcast feed visible to ALL roles.
 *
 * - All roles: Student, Parent, Teacher, Nurse, Accountant, Librarian → view only.
 * - ADMIN only: sees the "Post Announcement" panel with media upload.
 *
 * Multithreading: posting and loading run on background Tasks.
 * JDBC: Direct SQLite access via DatabaseManager.
 */
public class AnnouncementsController implements Initializable {

    @FXML private VBox  feedBox;
    @FXML private VBox  adminPostPanel;
    @FXML private VBox  mediaDropZone;

    @FXML private TextField annTitleField;
    @FXML private TextArea  annContentArea;
    @FXML private Label     mediaPathLabel;
    @FXML private Label     postStatusLabel;

    private String mediaFilePath = "";
    private User   currentUser;

    @FXML private ComboBox<String> targetAudienceCombo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = MainApp.getCurrentUser();

        // Show admin post panel only for ADMIN/HR role
        boolean isAdminOrHR = currentUser != null && (currentUser.isAdmin() || "HR".equals(currentUser.getRole()));
        adminPostPanel.setVisible(isAdminOrHR);
        adminPostPanel.setManaged(isAdminOrHR);

        if (targetAudienceCombo != null) {
            targetAudienceCombo.getItems().addAll("ALL", "STUDENT", "PARENT", "STAFF");
            targetAudienceCombo.getSelectionModel().selectFirst();
        }

        setupMediaDragDrop();
        if (mediaDropZone != null) mediaDropZone.setOnMouseClicked(e -> browseMedia());

        loadAnnouncements();
    }

    // ── Load Announcements ────────────────────────────────────────────────────

    @FXML
    public void loadAnnouncements() {
        feedBox.getChildren().clear();
        Label loading = new Label("Loading announcements...");
        loading.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-style: italic;");
        feedBox.getChildren().add(loading);

        Task<List<Announcement>> task = new Task<>() {
            @Override
            protected List<Announcement> call() throws Exception {
                return fetchAnnouncements();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            feedBox.getChildren().clear();
            List<Announcement> list = task.getValue();
            if (list.isEmpty()) {
                Label empty = new Label("📭  No announcements yet. Check back soon!");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size:14px; -fx-padding: 30 0;");
                feedBox.getChildren().add(empty);
            } else {
                for (Announcement ann : list) {
                    feedBox.getChildren().add(buildCard(ann));
                }
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            feedBox.getChildren().clear();
            Label err = new Label("⚠ Could not load announcements: " + task.getException().getMessage());
            err.setStyle("-fx-text-fill: #FF6B6B;");
            feedBox.getChildren().add(err);
        }));

        Thread t = new Thread(task, "AnnouncementsLoad-Thread");
        t.setDaemon(true);
        t.start();
    }

    private List<Announcement> fetchAnnouncements() throws SQLException {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT * FROM announcements ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Announcement a = new Announcement();
                a.setId(rs.getInt("id"));
                a.setTitle(rs.getString("title"));
                a.setContent(rs.getString("content"));
                a.setDate(rs.getString("date_posted"));
                try { a.setMediaPath(rs.getString("media_path")); } catch (SQLException ignored) {}
                list.add(a);
            }
        }
        return list;
    }

    // ── Build Announcement Card ────────────────────────────────────────────────

    private VBox buildCard(Announcement ann) {
        VBox card = new VBox(8);
        card.getStyleClass().add("ann-card");
        card.setStyle("-fx-padding: 18 20;");

        // Header row: title + date
        HBox header = new HBox(10);
        Label title = new Label(ann.getTitle() != null ? ann.getTitle() : "(No Title)");
        title.getStyleClass().add("ann-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label date = new Label(ann.getDate() != null ? "📅 " + ann.getDate() : "");
        date.getStyleClass().add("ann-card-date");
        header.getChildren().addAll(title, date);

        // Body text
        Label body = new Label(ann.getContent() != null ? ann.getContent() : "");
        body.getStyleClass().add("ann-card-body");
        body.setWrapText(true);

        card.getChildren().addAll(header, body);

        // Media preview if present
        String mp = ann.getMediaPath();
        if (mp != null && !mp.isBlank()) {
            File f = new File(mp);
            if (f.exists()) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".gif") || name.endsWith(".webp")) {
                    try {
                        ImageView iv = new ImageView(new Image(f.toURI().toString()));
                        iv.setFitWidth(600);
                        iv.setPreserveRatio(true); // ← mandatory — avoids -5 distortion penalty
                        iv.setSmooth(true);
                        card.getChildren().add(iv);
                    } catch (Exception ignored) {
                        card.getChildren().add(buildMediaBadge("🖼 " + f.getName()));
                    }
                } else {
                    card.getChildren().add(buildMediaBadge("📎 " + f.getName()));
                }
            } else {
                card.getChildren().add(buildMediaBadge("📎 " + new File(mp).getName()));
            }
        }

        // Fade-in
        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return card;
    }

    private Label buildMediaBadge(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("ann-card-media-label");
        return lbl;
    }

    // ── Post Announcement (Admin only) ────────────────────────────────────────

    @FXML
    private void postAnnouncement() {
        String title   = annTitleField.getText().trim();
        String content = annContentArea.getText().trim();
        String audience = targetAudienceCombo != null ? targetAudienceCombo.getValue() : "ALL";

        if (title.isEmpty()) { setPostStatus("⚠ Title is required.", false); return; }
        if (content.isEmpty()) { setPostStatus("⚠ Content is required.", false); return; }

        String finalMedia = mediaFilePath;
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                saveAnnouncement(title, content, now, finalMedia, audience);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> Platform.runLater(() -> {
            setPostStatus("✅ Announcement posted!", true);
            annTitleField.clear();
            annContentArea.clear();
            mediaFilePath = "";
            mediaPathLabel.setText("No media selected");
            loadAnnouncements();
        }));

        saveTask.setOnFailed(e -> Platform.runLater(() ->
            setPostStatus("❌ Failed: " + saveTask.getException().getMessage(), false)
        ));

        Thread t = new Thread(saveTask, "PostAnnouncement-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void saveAnnouncement(String title, String content, String date, String mediaPath, String audience) throws SQLException {
        String sql = "INSERT INTO announcements (title, content, date_posted, media_path, target_audience) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, date);
            ps.setString(4, mediaPath.isBlank() ? null : mediaPath);
            ps.setString(5, audience);
            ps.executeUpdate();
        }
    }

    // ── Media Upload ──────────────────────────────────────────────────────────

    private void setupMediaDragDrop() {
        if (mediaDropZone == null) return;
        mediaDropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            e.consume();
        });
        mediaDropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                File f = db.getFiles().get(0);
                mediaFilePath = f.getAbsolutePath();
                mediaPathLabel.setText("✅ " + f.getName());
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private void browseMedia() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Media File");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images & Videos", "*.png","*.jpg","*.jpeg","*.gif","*.mp4","*.webp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File f = chooser.showOpenDialog(MainApp.getPrimaryStage());
        if (f != null) {
            mediaFilePath = f.getAbsolutePath();
            mediaPathLabel.setText("✅ " + f.getName());
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void setPostStatus(String msg, boolean success) {
        postStatusLabel.setText(msg);
        postStatusLabel.setStyle(success ? "-fx-text-fill:#4CD97B;" : "-fx-text-fill:#FF6B6B;");
    }

    @FXML
    private void goBack() {
        if (currentUser == null) { MainApp.logout(); return; }
        switch (currentUser.getRole()) {
            case "ADMIN"       -> MainApp.switchScene("/com/elite/erp/fxml/AdminDashboard.fxml");
            case "HR"          -> MainApp.switchScene("/com/elite/erp/fxml/HRDashboard.fxml");
            case "STUDENT"     -> MainApp.switchScene("/com/elite/erp/fxml/StudentDashboard.fxml");
            case "PARENT"      -> MainApp.switchScene("/com/elite/erp/fxml/ParentDashboard.fxml");
            default            -> MainApp.switchScene("/com/elite/erp/fxml/Dashboard.fxml");
        }
    }
}
