package com.DevScribe.ui.screen;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LauncherScreen {

    private double xOffset = 0;
    private double yOffset = 0;

    public void start(Stage stage) {

        BorderPane root = new BorderPane();
//        root.setPadding(new Insets(15));

        HBox header = launchHeader(stage);
        root.setTop(header);

        VBox leftNav = leftNav();
        root.setLeft(leftNav);

        ScrollPane contentArea = createContentArea();
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 850, 725);
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private HBox launchHeader(Stage stage) {
        HBox titleBar = new HBox(); // Removed initial spacing
        titleBar.setStyle("-fx-background-color: #2c3333; -fx-padding: 5px;");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        // 1. Logo
        ImageView logoView = new ImageView(
                new Image(getClass().getResourceAsStream("/images/logo.png"))
        );
        logoView.setFitHeight(25);
        logoView.setPreserveRatio(true);

        // 2. Title
        Label title = new Label("DevScribe Launcher");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        title.setPadding(new Insets(0, 0, 0, 3));

        // 3. Group logo and title together first
        HBox logoTitleGroup = new HBox(3);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);
        logoTitleGroup.getChildren().addAll(logoView, title);

        // 3. Spacer and buttons
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window control buttons
        Button minimizeButton = createTitleBarButton("\uE921", stage, () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", stage, () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage, stage::close);

        titleBar.getChildren().addAll(logoView, title, spacer, minimizeButton, maximizeButton, closeButton);

        // Draggable window
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
    }

    // Helper method for consistent buttons
    private Button createTitleBarButton(String symbol, Stage stage, Runnable action) {
        Button btn = new Button(symbol);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.setOnAction(e -> action.run());

        // Hover effects
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3d4a4a; -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));

        // Special red hover for close button
        if (symbol.equals("\uE8BB")) {
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #e81123; -fx-text-fill: white;"));
        }

        return btn;
    }

    private VBox leftNav() {
        VBox leftNav = new VBox(20);
        leftNav.setPadding(new Insets(20));
        leftNav.setPrefWidth(250);

        // Logo
        ImageView leftNavLogo = new ImageView(
                new Image(getClass().getResourceAsStream("/images/logo.png"))
        );
        leftNavLogo.setFitHeight(50);
        leftNavLogo.setPreserveRatio(true);

        // Title
        Label leftNavTitle = new Label("DevScribe");
        leftNavTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 25px;");

        HBox leftNavLogoTitle = new HBox(10);
        leftNavLogoTitle.getChildren().addAll(leftNavLogo, leftNavTitle);

        leftNav.getChildren().addAll(leftNavLogoTitle);

        leftNav.setStyle("-fx-background-color:#474A48;");

        return leftNav;
    }

    //main area color : #909590
    private ScrollPane createContentArea(){
        ScrollPane contentArea = new ScrollPane();
        contentArea.setFitToWidth(true);
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // 3. Welcome message
        Label welcome = new Label("Welcome to DevScribe");
        welcome.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");


        content.getChildren().addAll(welcome);
        contentArea.setContent(content);
        return contentArea;
    }


}