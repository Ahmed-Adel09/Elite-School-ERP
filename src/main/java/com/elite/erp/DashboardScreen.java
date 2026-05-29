package com.elite.erp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * DashboardScreen — Programmatic JavaFX Dashboard for Vertex Academy ERP.
 * Built using pure Java code (No FXML).
 */
public class DashboardScreen extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f8f9fa;");

        // ─── TOP NAVIGATION BAR ───
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 25, 10, 25));
        topBar.setPrefHeight(70);
        topBar.setStyle("-fx-background-color: #001f3f;"); // Navy

        Label brandLabel = new Label("VERTEX ERP — ADMIN DASHBOARD");
        brandLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brandLabel.setStyle("-fx-text-fill: #FFD700;"); // Gold

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("LOGOUT");
        btnLogout.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> System.out.println("Logging out..."));

        topBar.getChildren().addAll(brandLabel, spacer, btnLogout);
        root.setTop(topBar);

        // ─── SIDEBAR ───
        VBox sideBar = new VBox(15);
        sideBar.setPadding(new Insets(30, 20, 20, 20));
        sideBar.setPrefWidth(240);
        sideBar.setStyle("-fx-background-color: #f1f2f6; -fx-border-color: #dfe4ea; -fx-border-width: 0 1 0 0;");

        String[] navItems = {"Dashboard", "Student Records", "Teacher Management", "Fee Collection", "Examination", "Settings"};
        for (String item : navItems) {
            Button navBtn = new Button(item);
            navBtn.setMaxWidth(Double.MAX_VALUE);
            navBtn.setAlignment(Pos.CENTER_LEFT);
            navBtn.setPrefHeight(45);
            navBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2f3542; -fx-font-size: 14px; -fx-cursor: hand;");
            
            navBtn.setOnMouseEntered(e -> navBtn.setStyle("-fx-background-color: #dfe4ea; -fx-text-fill: #001f3f; -fx-font-size: 14px; -fx-cursor: hand;"));
            navBtn.setOnMouseExited(e -> navBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2f3542; -fx-font-size: 14px; -fx-cursor: hand;"));
            
            sideBar.getChildren().add(navBtn);
        }
        root.setLeft(sideBar);

        // ─── MAIN CONTENT AREA ───
        VBox mainContent = new VBox(30);
        mainContent.setPadding(new Insets(40));
        
        Label welcomeText = new Label("Welcome back, Administrator");
        welcomeText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        welcomeText.setStyle("-fx-text-fill: #2f3542;");

        // Summary Cards Container
        HBox cardsContainer = new HBox(25);
        
        cardsContainer.getChildren().addAll(
            createSummaryCard("Total Students", "1,248", "#2980b9"),
            createSummaryCard("Active Teachers", "86", "#27ae60"),
            createSummaryCard("Revenue (MTD)", "$42,500", "#f39c12")
        );

        mainContent.getChildren().addAll(welcomeText, cardsContainer);
        root.setCenter(mainContent);

        // ─── STAGE CONFIG ───
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Vertex Academy ERP — Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSummaryCard(String title, String value, String colorHex) {
        VBox card = new VBox(10);
        card.setPrefSize(250, 150);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #dfe4ea; " +
            "-fx-border-radius: 10;"
        );

        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0,0,0,0.05));
        ds.setRadius(10);
        ds.setOffsetY(5);
        card.setEffect(ds);

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblTitle.setStyle("-fx-text-fill: #747d8c;");

        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblValue.setStyle("-fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
