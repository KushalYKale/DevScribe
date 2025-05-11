package com.DevScribe.utils;

import javafx.scene.control.Alert;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathValidator {

    public static boolean validateProjectPath(Path projectPath) {
        if (projectPath == null) {
            showErrorDialog("Error", "Project path is null.");
            return false;
        }
        if (!Files.exists(projectPath)) {
            showErrorDialog("Error", "Project path does not exist.");
            return false;
        }
        if (!Files.isDirectory(projectPath)) {
            showErrorDialog("Error", "Project path is not a directory.");
            return false;
        }
        return true;
    }

    private static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
