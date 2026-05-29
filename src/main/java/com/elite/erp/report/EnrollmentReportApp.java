package com.elite.erp.report;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.elite.erp.business.StudentService;
import com.elite.erp.util.Response;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * EnrollmentReportApp — FXGL Game Application for the Reports module.
 *
 * Demonstrates the mandatory FXGL requirement:
 *  - Extends GameApplication (FXGL entry point)
 *  - Uses addUINode() to embed a real JavaFX BarChart into the FXGL scene
 *  - Connects to the SQLite database via StudentService to display REAL data
 *  - Uses onUpdate() loop (FXGL game loop) to simulate real-time data refresh
 *
 * Launched from the ERP dashboard in a separate thread.
 */
public class EnrollmentReportApp extends GameApplication {

    private StudentService studentService;
    private BarChart<String, Number> barChart;
    private XYChart.Series<String, Number> series;
    private double refreshTimer = 0;
    private static final double REFRESH_INTERVAL = 10.0; // seconds

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Elite School ERP — Enrollment Report");
        settings.setVersion("1.0");
        settings.setWidth(900);
        settings.setHeight(620);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
        settings.setProfilingEnabled(false);
    }

    @Override
    protected void initGame() {
        studentService = new StudentService();
        buildChartUI();
    }

    private void buildChartUI() {
        // ── Title label ──────────────────────────────────────────────────────
        Label title = new Label("📊  Student Enrollment Trends — 2025");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#C9A84C")); // Gold
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 2);");

        Label subtitle = new Label("Monthly new admissions pulled live from SQLite database");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web("#CCCCCC"));

        // ── Axes ─────────────────────────────────────────────────────────────
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        xAxis.setTickLabelFill(Color.WHITE);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Students Enrolled");
        yAxis.setTickLabelFill(Color.WHITE);

        // ── BarChart ─────────────────────────────────────────────────────────
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Monthly Enrollment");
        barChart.setLegendVisible(false);
        barChart.setPrefSize(860, 430);
        barChart.setStyle("""
            -fx-background-color: transparent;
            -fx-plot-background-color: #0D1F3C;
            -fx-text-fill: white;
            -fx-title-fill: white;
            """);

        series = new XYChart.Series<>();
        series.setName("Enrollments");
        barChart.getData().add(series);
        loadChartData();

        // ── Status label ─────────────────────────────────────────────────────
        Label status = new Label("⟳  Auto-refreshing every 10 seconds from live database");
        status.setFont(Font.font("Segoe UI", 11));
        status.setTextFill(Color.web("#888888"));

        // ── Layout ───────────────────────────────────────────────────────────
        VBox layout = new VBox(12, title, subtitle, barChart, status);
        layout.setLayoutX(20);
        layout.setLayoutY(20);
        layout.setStyle("-fx-background-color: #0A1628; -fx-padding: 20;");
        layout.setPrefSize(860, 580);

        // ── FXGL: add JavaFX node to FXGL UI scene ───────────────────────────
        addUINode(layout); // <-- mandatory FXGL requirement
    }

    private void loadChartData() {
        Response<Map<String, Integer>> resp = studentService.getEnrollmentStats();
        if (resp.isSuccess()) {
            Platform.runLater(() -> {
                series.getData().clear();
                resp.getData().forEach((month, count) -> {
                    XYChart.Data<String, Number> bar = new XYChart.Data<>(month, count);
                    series.getData().add(bar);
                });
                // Style each bar gold after rendering
                barChart.lookupAll(".bar").forEach(node ->
                    node.setStyle("-fx-bar-fill: #C9A84C;")
                );
            });
        }
    }

    @Override
    protected void onUpdate(double tpf) {
        // FXGL game loop — demonstrates real-time update capability
        refreshTimer += tpf;
        if (refreshTimer >= REFRESH_INTERVAL) {
            refreshTimer = 0;
            loadChartData(); // refresh chart from DB every 10 s
        }
    }

    /**
     * Launch the FXGL report window in a separate thread from the main ERP.
     * This prevents blocking the JavaFX main thread.
     */
    public static void launchInThread() {
        Thread reportThread = new Thread(() ->
            GameApplication.launch(EnrollmentReportApp.class, new String[]{}),
            "FXGL-Report-Thread"
        );
        reportThread.setDaemon(true);
        reportThread.start();
    }
}
