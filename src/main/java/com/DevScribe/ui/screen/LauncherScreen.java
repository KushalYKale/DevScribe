package com.DevScribe.ui.screen;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LauncherScreen {
    private double xOffset = 0;
    private double yOffset = 0;

    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        root.setTop(createHeader(stage));
        root.setLeft(createLeftNav());
        root.setCenter(createContentArea());

        Scene scene = new Scene(root, 850, 725);
        scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private HBox createHeader(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(16);
        logoView.setPreserveRatio(true);

        Label title = new Label("DevScribe Launcher");
        title.getStyleClass().add("header-title");

        HBox logoTitleGroup = new HBox(3);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);
        logoTitleGroup.getChildren().addAll(logoView, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        titleBar.getChildren().addAll(logoTitleGroup, spacer, minimizeButton, maximizeButton, closeButton);

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

    private Button createTitleBarButton(String symbol, Runnable action) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add("title-bar-button");
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private VBox createLeftNav() {
        VBox leftNav = new VBox(20);
        leftNav.setPadding(new Insets(20));
        leftNav.setPrefWidth(250);
        leftNav.getStyleClass().add("left-nav");

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logo.setFitHeight(50);
        logo.setPreserveRatio(true);

        Label title = new Label("DevScribe");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 25px;");

        HBox logoTitle = new HBox(10);
        logoTitle.getChildren().addAll(logo, title);

        leftNav.getChildren().add(logoTitle);
        return leftNav;
    }

    private BorderPane createContentArea() {
        BorderPane contentArea = new BorderPane();
        contentArea.getStyleClass().add("content-area");

        contentArea.setTop(createToolbar());

        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setFitToWidth(true);
        scrollContent.getStyleClass().add("scroll-content");
        scrollContent.setContent(createMainContent());

        contentArea.setCenter(scrollContent);
        return contentArea;
    }

    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getStyleClass().add("main-content");
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(8, 15, 8, 15));
        toolbar.getStyleClass().add("toolbar");

        TextField searchField = new TextField();
        searchField.setPromptText("Search projects...");
        searchField.getStyleClass().add("search-field");

        HBox searchBox = new HBox(8);
        searchBox.getStyleClass().add("search-box");
        searchBox.getChildren().add(searchField);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Button newProjectBtn = createToolbarButton("New Project");
        Button openBtn = createToolbarButton("Open");
        Button cloneBtn = createToolbarButton("Clone");

        toolbar.getChildren().addAll(
                searchBox,
                new Separator(Orientation.VERTICAL),
                newProjectBtn,
                openBtn,
                cloneBtn
        );

        return toolbar;
    }

    private Button createToolbarButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("toolbar-button");
        return btn;
    }
}