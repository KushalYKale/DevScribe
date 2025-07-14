package com.DevScribe.ui.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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

    // The method that is used to show the New Project dialog
    public static void showNewProjectDialog(Stage parentStage, ProjectCreationCallback callback) {
        // Validate inputs
        if (parentStage == null || callback == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        // Show dialog to enter the project name
        TextInputDialog nameDialog = createNameDialog(parentStage);
        Optional<String> nameResult = nameDialog.showAndWait();

        // Process the entered project name
        nameResult.ifPresent(projectName -> {
            projectName = projectName.trim();  // Trim project name
            if (projectName.isEmpty()) {
                callback.onError("Project name cannot be empty");
                return;
            }

            // Show the directory chooser to select project location
            File selectedDir = showDirectoryChooser(parentStage);
            if (selectedDir != null) {
                // Create the project directory in the selected location
                createProjectDirectory(selectedDir, projectName, callback);
            }
        });
    }

    // Create the input dialog to enter project name
    private static TextInputDialog createNameDialog(Stage parentStage) {
        TextInputDialog dialog = new TextInputDialog("MyProject");
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create New Project");
        dialog.setContentText("Enter project name:");
        dialog.initOwner(parentStage);
        return dialog;
    }

    // Show a directory chooser for selecting the location of the new project
    private static File showDirectoryChooser(Stage parentStage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Project Location");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return chooser.showDialog(parentStage);
    }

    // Create the new project directory and handle errors
    private static void createProjectDirectory(File parentDir, String projectName,
                                               ProjectCreationCallback callback) {
        // Validate the project name to ensure it doesn't contain invalid characters
        if (!isValidProjectName(projectName)) {
            callback.onError("Invalid project name: " + projectName);
            return;
        }

        // Create the full project path
        Path projectPath = Paths.get(parentDir.getAbsolutePath(), projectName);

        try {
            // Check if a directory with the same name already exists
            if (Files.exists(projectPath)) {
                callback.onError("A folder with this name already exists");
            } else {
                // Create the new directory
                Files.createDirectory(projectPath);
                // Notify that the project was created successfully
                callback.onProjectCreated(projectPath);
            }
        } catch (IOException e) {
            callback.onError("Failed to create project: " + e.getMessage());
        } catch (InvalidPathException e) {
            callback.onError("Invalid project name: " + e.getMessage());
        }
    }

    // Validate that the project name does not contain invalid characters
    private static boolean isValidProjectName(String projectName) {
        // Basic validation to check for forbidden characters
        // You can add more rules if necessary
        String regex = "^[^<>:\"/\\|?*]+$";
        return projectName.matches(regex);
    }

    // Show an error alert to the user if something goes wrong
    private static void showErrorAlert(String errorMessage) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Project Creation Error");
        alert.setHeaderText("Error creating project");
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }
}
