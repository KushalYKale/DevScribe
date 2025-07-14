package com.DevScribe;

import com.DevScribe.ui.screen.LauncherScreen;
import com.DevScribe.ui.screen.SplashScreen;   // ⬅ add
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {

        new SplashScreen().show(new Stage(), () -> {
            Platform.runLater(() -> {                 // ensure we’re on the FX thread
                new LauncherScreen().start(primaryStage);
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
