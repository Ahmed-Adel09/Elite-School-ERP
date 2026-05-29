package com.elite.erp.presentation.announcements;

import com.elite.erp.dao.DatabaseManager;
import com.elite.erp.model.Announcement;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AnnouncementFeedWidget — OOP Reusable JavaFX Component.
 * Fetches and displays announcements for a specific target audience.
 */
public class AnnouncementFeedWidget extends VBox {

    private final String targetAudience;

    public AnnouncementFeedWidget(String targetAudience) {
        this.targetAudience = targetAudience;
        this.setSpacing(10);
        loadAnnouncements();
    }

    public void loadAnnouncements() {
        this.getChildren().clear();
        Label loading = new Label("Loading announcements...");
        loading.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-style: italic;");
        this.getChildren().add(loading);

        Task<List<Announcement>> task = new Task<>() {
            @Override
            protected List<Announcement> call() throws Exception {
                return fetchAnnouncements();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            this.getChildren().clear();
            List<Announcement> list = task.getValue();
            if (list.isEmpty()) {
                Label empty = new Label("📭  No announcements yet.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size:14px; -fx-padding: 30 0;");
                this.getChildren().add(empty);
            } else {
                for (Announcement ann : list) {
                    this.getChildren().add(buildCard(ann));
                }
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            this.getChildren().clear();
            Label err = new Label("⚠ Could not load announcements: " + task.getException().getMessage());
            err.setStyle("-fx-text-fill: #FF6B6B;");
            this.getChildren().add(err);
        }));

        Thread t = new Thread(task, "AnnouncementsFeed-Thread");
        t.setDaemon(true);
        t.start();
    }

    private List<Announcement> fetchAnnouncements() throws SQLException {
        List<Announcement> list = new ArrayList<>();
        String sql = "SELECT * FROM announcements WHERE target_audience = 'ALL' OR target_audience = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetAudience);
            try (ResultSet rs = ps.executeQuery()) {
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
        }
        return list;
    }

    private VBox buildCard(Announcement ann) {
        VBox card = new VBox(8);
        card.getStyleClass().add("ann-card");
        card.setStyle("-fx-padding: 18 20; -fx-background-color: #1A3560; -fx-background-radius: 8;");

        HBox header = new HBox(10);
        Label title = new Label(ann.getTitle() != null ? ann.getTitle() : "(No Title)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label date = new Label(ann.getDate() != null ? "📅 " + ann.getDate() : "");
        date.setStyle("-fx-text-fill: #94A3B8;");
        header.getChildren().addAll(title, date);

        Label body = new Label(ann.getContent() != null ? ann.getContent() : "");
        body.setStyle("-fx-text-fill: white;");
        body.setWrapText(true);

        card.getChildren().addAll(header, body);

        String mp = ann.getMediaPath();
        if (mp != null && !mp.isBlank()) {
            File f = new File(mp);
            if (f.exists()) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".gif") || name.endsWith(".webp")) {
                    try {
                        ImageView iv = new ImageView(new Image(f.toURI().toString()));
                        iv.setFitWidth(400);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        card.getChildren().add(iv);
                    } catch (Exception ignored) {
                        card.getChildren().add(new Label("🖼 " + f.getName()));
                    }
                } else {
                    card.getChildren().add(new Label("📎 " + f.getName()));
                }
            } else {
                card.getChildren().add(new Label("📎 " + new File(mp).getName()));
            }
        }

        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return card;
    }
}
