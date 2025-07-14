package com.DevScribe.ui.screen;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SplashScreen {
    public void show(Stage splashStage, Runnable onFinish) {
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #23232B; -fx-alignment: center; -fx-padding: 40;");

        ImageView logo = new ImageView("/images/logo.png");
        logo.setFitHeight(80);
        logo.setPreserveRatio(true);

        Label appName = new Label("DevScribe IDE");
        appName.setStyle("-fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold;");

        Label loading = new Label("Loading, please wait...");
        loading.setStyle("-fx-text-fill: gray; -fx-font-size: 14;");

        root.getChildren().addAll(logo, appName, loading);

        Scene scene = new Scene(root, 400, 250);
        splashStage.setScene(scene);
        splashStage.setResizable(false);
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        FadeTransition fade = new FadeTransition(Duration.seconds(2), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setOnFinished(e -> {
            // Wait 2 more seconds, then call the main app
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        splashStage.close();
                        onFinish.run(); // Launch EditorScreen
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        });
        fade.play();
    }
}

