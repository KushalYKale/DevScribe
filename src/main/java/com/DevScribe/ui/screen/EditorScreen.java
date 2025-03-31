package com.DevScribe.ui.screen;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EditorScreen {
    private double xOffset = 0;
    private double yOffset = 0;
    private BorderPane root;

    public void start(Stage stage) {
        root = new BorderPane();  // Initialize the class field
        root.setTop(createHeader(stage));

        Button close = new Button("Close");
        close.setOnAction(e -> {
            Stage newStage = new Stage();
            LauncherScreen launcherScreen = new LauncherScreen();
            try {
                launcherScreen.start(newStage);
                stage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        root.setBottom(close);

        Scene scene = new Scene(root, 1400, 750);
        scene.getStylesheets().add(getClass().getResource("/css/editor.css").toExternalForm());
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

        Label title = new Label("DevScribe");
        title.getStyleClass().add("header-title");

        HBox logoTitleGroup = new HBox(3);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);
        logoTitleGroup.getChildren().addAll(logoView, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button runButton = createTitleBarButton("\u25B6", () -> {
            // Add functionality to run the code
            System.out.println("Run button clicked");
        });
        runButton.getStyleClass().add("run-button");

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        HBox buttonContainer = new HBox(8);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.getChildren().addAll(
                runButton,
                minimizeButton,
                maximizeButton,
                closeButton
        );

        titleBar.getChildren().addAll(logoTitleGroup, spacer, buttonContainer);

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
}