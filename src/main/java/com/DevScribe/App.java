package com.DevScribe;

import com.DevScribe.ui.screen.EditorScreen;
import com.DevScribe.ui.screen.LauncherScreen;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        new LauncherScreen().start(stage);
//        Path projectPath = Path.of("C:\\Users\\Kushal\\OneDrive\\Desktop\\MyProject");
//        new EditorScreen().start(stage,projectPath);
    }


    public static void main(String[] args) {
        launch(args);
    }
}