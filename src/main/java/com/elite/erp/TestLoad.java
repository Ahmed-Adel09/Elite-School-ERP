package com.elite.erp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestLoad extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/elite/erp/fxml/AdminDashboard.fxml"));
            Parent root = loader.load();
            System.out.println("SUCCESSFULLY LOADED!");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("FAILED TO LOAD:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
