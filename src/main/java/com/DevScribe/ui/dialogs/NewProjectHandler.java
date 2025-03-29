package com.DevScribe.ui.dialogs;

import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NewProjectHandler {  // Renamed to follow Java class naming conventions

    public interface ProjectCreationCallback {
        void onProjectCreated(Path projectPath);
        void onError(String errorMessage);
    }

    public static void showNewProjectDialog(Stage parentStage, ProjectCreationCallback callback) {
        // Validate inputs
        if (parentStage == null || callback == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        TextInputDialog nameDialog = createNameDialog(parentStage);
        Optional<String> nameResult = nameDialog.showAndWait();

        nameResult.ifPresent(projectName -> {
            File selectedDir = showDirectoryChooser(parentStage);
            if (selectedDir != null) {
                createProjectDirectory(selectedDir, projectName.trim(), callback);
            }
        });
    }

    private static TextInputDialog createNameDialog(Stage parentStage) {
        TextInputDialog dialog = new TextInputDialog("MyProject");
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create New Project");
        dialog.setContentText("Enter project name:");
        dialog.initOwner(parentStage);
        return dialog;
    }

    private static File showDirectoryChooser(Stage parentStage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Project Location");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return chooser.showDialog(parentStage);
    }

    private static void createProjectDirectory(File parentDir, String projectName,
                                               ProjectCreationCallback callback) {
        if (projectName.isEmpty()) {
            callback.onError("Project name cannot be empty");
            return;
        }

        Path projectPath = Paths.get(parentDir.getAbsolutePath(), projectName);

        try {
            if (projectPath.toFile().exists()) {
                callback.onError("A folder with this name already exists");
            } else {
                Files.createDirectory(projectPath);
                callback.onProjectCreated(projectPath);
            }
        } catch (IOException e) {
            callback.onError("Failed to create project: " + e.getMessage());
        } catch (InvalidPathException e) {
            callback.onError("Invalid project name: " + e.getMessage());
        }
    }
}