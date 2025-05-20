package com.DevScribe.utils;

import com.DevScribe.ui.screen.EditorScreen;
import com.DevScribe.ui.screen.LauncherScreen;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.nio.file.Path;

public class ScreenManager {

    static boolean isDarkMode = true;

    public static void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
    }

    public static void switchToEditor(Stage currentStage, Path projectPath) {
        try {
            Stage editorStage = new Stage();
            EditorScreen editor = new EditorScreen();
            editor.start(editorStage, projectPath,isDarkMode);
            currentStage.close();
        } catch (Exception e) {
            showError("Failed to open editor", e);
        }
    }

    public static void switchToLauncher(Stage currentStage) {
        try {
            Stage launcherStage = new Stage();
            LauncherScreen launcher = new LauncherScreen();
            launcher.start(launcherStage);
            currentStage.close();
        } catch (Exception e) {
            showError("Failed to open launcher", e);
        }
    }

    private static void showError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}