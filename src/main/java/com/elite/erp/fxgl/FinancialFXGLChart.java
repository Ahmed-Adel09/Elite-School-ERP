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
 * FinancialFXGLChart — Embeds an FXGL BarChart inside the Accountant Dashboard.
 *
 * Queries the invoices table and displays two bars:
 *   "Collected Revenue"  (status = PAID)
 *   "Outstanding Dues"   (status = PENDING)
 *
 * AnimationTimer polls DB every 3 seconds for live refresh.
 * Mouse event overlay prevents FXGL InputHandler crash.
 */
public class FinancialFXGLChart extends GameApplication {

    private BarChart<String, Number> barChart;
    private static FinancialFXGLChart instance;

    public FinancialFXGLChart() { instance = this; }

    public static FinancialFXGLChart getInstance() {
        if (instance == null) instance = new FinancialFXGLChart();
        return instance;
    }

    /** Builds chart and embeds it into the given StackPane. */
    public void embedInto(StackPane parentPane) {
        buildChart();

        // Transparent overlay — consumes all mouse events before FXGL sees them
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPickOnBounds(true);
        overlay.addEventFilter(MouseEvent.ANY, javafx.event.Event::consume);

        parentPane.getChildren().setAll(barChart, overlay);

        // AnimationTimer polls every 3 s
        new AnimationTimer() {
            private long lastUpdate = 0;
            @Override public void handle(long now) {
                if (now - lastUpdate >= 3_000_000_000L) {
                    fetchAndRender();
                    lastUpdate = now;
                }
            }
        }.start();

        fetchAndRender();
        System.out.println("[FXGL] FinancialFXGLChart AnimationTimer active.");
    }

    private void buildChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Financial Status");
        xAxis.setTickLabelFill(Color.web("#C9A84C"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (USD)");
        yAxis.setTickLabelFill(Color.web("#C9A84C"));

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Live Financial Overview");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefSize(700, 360);
        barChart.setStyle("-fx-background-color: #0A1628; -fx-text-fill: #C9A84C;");
    }

    private void fetchAndRender() {
        double paid    = querySum("PAID");
        double pending = querySum("PENDING");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        XYChart.Data<String, Number> paidBar    = new XYChart.Data<>("✅ Collected Revenue", paid);
        XYChart.Data<String, Number> pendingBar = new XYChart.Data<>("⚠️ Outstanding Dues", pending);
        series.getData().addAll(paidBar, pendingBar);

        Platform.runLater(() -> {
            barChart.getData().clear();
            barChart.getData().add(series);

            // Colour bars after JavaFX lays them out
            Platform.runLater(() -> {
                if (paidBar.getNode()    != null)
                    paidBar.getNode().setStyle("-fx-bar-fill: #4CD97B;");
                if (pendingBar.getNode() != null)
                    pendingBar.getNode().setStyle("-fx-bar-fill: #FF6B6B;");
            });
        });
    }

    private double querySum(String status) {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM invoices WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) {
            System.err.println("[FinancialChart] querySum error: " + e.getMessage());
        }
        return 0.0;
    }

    // ── FXGL GameApplication lifecycle ───────────────────────────────────────

    @Override protected void initSettings(GameSettings s) {
        s.setWidth(700); s.setHeight(360);
        s.setTitle("Vertex Academy Financial Engine");
    }

    @Override protected void initGame() {
        System.out.println("[FXGL] FinancialFXGLChart engine initialised.");
    }

    @Override protected void initUI() {
        if (barChart == null) buildChart();
    }
}
