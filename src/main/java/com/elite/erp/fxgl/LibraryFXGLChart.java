package com.elite.erp.fxgl;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.elite.erp.dao.DatabaseManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * LibraryFXGLChart — FXGL integrated bar chart for Librarian Dashboard.
 * Shows Top 5 Borrowed Categories.
 */
public class LibraryFXGLChart extends GameApplication {

    private BarChart<String, Number> barChart;
    private static LibraryFXGLChart instance;

    public LibraryFXGLChart() {
        instance = this;
    }

    public static LibraryFXGLChart getInstance() {
        if (instance == null) instance = new LibraryFXGLChart();
        return instance;
    }

    public void embedInto(StackPane parentPane) {
        buildChart();

        // Transparent overlay - mouse event interceptor to prevent FXGL crash
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPickOnBounds(true);
        overlay.addEventFilter(MouseEvent.ANY, javafx.event.Event::consume);

        parentPane.getChildren().setAll(barChart, overlay);

        // AnimationTimer polls DB every 5 seconds for top borrowed categories
        new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 5_000_000_000L) {
                    fetchAndRenderData();
                    lastUpdate = now;
                }
            }
        }.start();

        tryStartFXGLTimer();
    }

    private void buildChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");
        xAxis.setTickLabelFill(Color.web("#C9A84C"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Times Borrowed");
        yAxis.setTickLabelFill(Color.web("#C9A84C"));

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Top 5 Borrowed Categories");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setStyle("-fx-text-fill: white;");
        
        barChart.lookup(".chart-title").setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
    }

    private void fetchAndRenderData() {
        String sql = """
            SELECT lb.category, COUNT(bl.id) as borrow_count 
            FROM book_loans bl
            JOIN library_books lb ON bl.book_id = lb.id
            GROUP BY lb.category
            ORDER BY borrow_count DESC
            LIMIT 5
        """;

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String cat = rs.getString("category");
                int count = rs.getInt("borrow_count");
                series.getData().add(new XYChart.Data<>(cat, count));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {
            barChart.getData().clear();
            barChart.getData().add(series);
            // Apply custom styling to the bars
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: #C9A84C;");
                }
            }
        });
    }

    private void tryStartFXGLTimer() {
        Thread t = new Thread(() -> {
            try { GameApplication.embeddedLaunch(this); }
            catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(100);
        settings.setHeight(100);
        settings.setTitle("Library Analytics");
    }

    @Override
    protected void initGame() {}
}
