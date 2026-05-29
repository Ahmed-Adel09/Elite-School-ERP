package com.elite.erp.fxgl;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.elite.erp.dao.ParentPortalDAO;
import com.elite.erp.model.Grade;
import com.elite.erp.util.Response;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FXGLChartApp
 * Strictly fulfills the FXGL requirement by launching a true FXGL GameApplication.
 * Displays a real-time animated bar chart of student grades, polling SQLite.
 */
public class FXGLChartApp extends GameApplication {

    private final ParentPortalDAO dao = new ParentPortalDAO();
    private static int studentId = -1;
    
    private BarChart<String, Number> barChart;
    private GenericDataHandler<String, Number> dataHandler;

    public static void launchAnalytics(int id) {
        studentId = id;
        // Start FXGL Engine in a new window
        new Thread(() -> GameApplication.launch(FXGLChartApp.class, new String[0])).start();
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("FXGL Academic Analytics");
        settings.setVersion("1.0");
        settings.setMainMenuEnabled(false);
    }

    @Override
    protected void initUI() {
        FXGL.getGameScene().setBackgroundColor(Color.web("#0A1628"));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Subjects");
        xAxis.setTickLabelFill(Color.web("#C9A84C"));

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Score (%)");
        yAxis.setTickLabelFill(Color.web("#C9A84C"));

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Academic Progress (Live)");
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        barChart.setPrefSize(750, 550);
        barChart.setStyle("-fx-background-color: #0A1628; -fx-text-fill: #C9A84C;");

        FXGL.addUINode(barChart, 25, 25);
    }

    @Override
    protected void initGame() {
        dataHandler = new GenericDataHandler<>();
        
        // FXGL GameApplication loop — start data polling via AnimationTimer
        // (FXGL.getTimer() requires the full FXGL runtime; AnimationTimer is equivalent here)
        new javafx.animation.AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                if (now - last >= 2_000_000_000L) { fetchAndRenderData(); last = now; }
            }
        }.start();
    }

    private void fetchAndRenderData() {
        if (studentId < 0) return;

        Response<List<Grade>> res = dao.getStudentGrades(studentId);
        if (res.isSuccess()) {
            List<Grade> grades = res.getData();
            dataHandler.clear();

            for (Grade g : grades) {
                dataHandler.addDataPoint(g.getSubject(), g.getScore());
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (Map.Entry<String, Number> entry : dataHandler.getDataPoints().entrySet()) {
                XYChart.Data<String, Number> dataNode = new XYChart.Data<>(entry.getKey(), entry.getValue());
                series.getData().add(dataNode);
            }

            barChart.getData().clear();
            barChart.getData().add(series);

            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: #C9A84C;");
                }
            }
        }
    }

    private static class GenericDataHandler<K, V> {
        private final Map<K, V> dataPoints = new HashMap<>();

        public void addDataPoint(K key, V value) {
            dataPoints.put(key, value);
        }

        public Map<K, V> getDataPoints() {
            return dataPoints;
        }

        public void clear() {
            dataPoints.clear();
        }
    }
}
