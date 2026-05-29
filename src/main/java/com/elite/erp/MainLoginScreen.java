package com.elite.erp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;


public class MainLoginScreen extends Application {

    @Override
    public void start(Stage primaryStage) {
        // ─── ROOT LAYOUT ───
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #f0f2f5;"); // Modern light grey background

        // ─── LOGIN CARD (VBox) ───
        VBox loginCard = new VBox(25);
        loginCard.setAlignment(Pos.TOP_CENTER);
        loginCard.setMaxSize(400, 500);
        loginCard.setPadding(new Insets(0, 0, 40, 0));
        
        // Premium Card Styling
        loginCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-border-radius: 15; " +
            "-fx-border-color: #dcdde1; " +
            "-fx-border-width: 1;"
        );

        // Subtle Drop Shadow Effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        loginCard.setEffect(shadow);

        // ─── BRANDING HEADER ───
        Label headerLabel = new Label("VERTEX ACADEMY ERP");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setPrefHeight(80);
        
        // Deep Navy Background with Rich Gold Text
        headerLabel.setStyle(
            "-fx-background-color: #001f3f; " + // Deep Navy
            "-fx-text-fill: #FFD700; " +        // Rich Gold
            "-fx-background-radius: 15 15 0 0;" // Round top corners only
        );

        // ─── INPUT FIELDS ───
        VBox inputContainer = new VBox(15);
        inputContainer.setPadding(new Insets(10, 40, 0, 40));

        Label welcomeLabel = new Label("Sign In to Portal");
        welcomeLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        welcomeLabel.setStyle("-fx-text-fill: #2f3640;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username or Email");
        usernameField.setPrefHeight(45);
        usernameField.setStyle(
            "-fx-background-radius: 5; " +
            "-fx-border-color: #dcdde1; " +
            "-fx-border-radius: 5; " +
            "-fx-font-size: 14px;"
        );

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefHeight(45);
        passwordField.setStyle(
            "-fx-background-radius: 5; " +
            "-fx-border-color: #dcdde1; " +
            "-fx-border-radius: 5; " +
            "-fx-font-size: 14px;"
        );

        // ─── LOGIN BUTTON ───
        Button btnSignIn = new Button("SIGN IN");
        btnSignIn.setMaxWidth(Double.MAX_VALUE);
        btnSignIn.setPrefHeight(45);
        btnSignIn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        // Rich Gold Styling
        btnSignIn.setStyle(
            "-fx-background-color: #FFD700; " + // Gold
            "-fx-text-fill: #001f3f; " +        // Navy text
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        // Hover Effect Logic (Programmatic)
        btnSignIn.setOnMouseEntered(e -> btnSignIn.setStyle("-fx-background-color: #e6c200; -fx-text-fill: #001f3f; -fx-background-radius: 5; -fx-cursor: hand;"));
        btnSignIn.setOnMouseExited(e -> btnSignIn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #001f3f; -fx-background-radius: 5; -fx-cursor: hand;"));

        // ─── FEEDBACK LABEL ───
        Label feedbackLabel = new Label("");
        feedbackLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
        feedbackLabel.setWrapText(true);
        feedbackLabel.setAlignment(Pos.CENTER);

        // ─── ACTION LOGIC ───
        btnSignIn.setOnAction(event -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();

            if ("admin".equalsIgnoreCase(user) && "admin".equals(pass)) {
                feedbackLabel.setText("Login Successful! Redirecting...");
                feedbackLabel.setStyle("-fx-text-fill: #27ae60;"); // Green
            } else {
                feedbackLabel.setText("Invalid Credentials. Please try again.");
                feedbackLabel.setStyle("-fx-text-fill: #c0392b;"); // Red
            }
        });

        // ─── ASSEMBLY ───
        inputContainer.getChildren().addAll(welcomeLabel, usernameField, passwordField, btnSignIn, feedbackLabel);
        loginCard.getChildren().addAll(headerLabel, inputContainer);
        root.getChildren().add(loginCard);

        // ─── STAGE CONFIGURATION ───
        Scene scene = new Scene(root, 1000, 680);
        primaryStage.setTitle("Vertex Academy ERP — Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
