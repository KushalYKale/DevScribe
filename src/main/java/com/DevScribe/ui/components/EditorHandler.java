package com.DevScribe.ui.components;

import com.DevScribe.ui.screen.EditorScreen;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class EditorHandler {

    private final EditorScreen editorScreen;
    private final Map<Tab, File> tabFileMap = new HashMap<>();
    private Path projectDirectory; // Project directory, now dynamic
    private TreeView<Path> projectTreeView; // The TreeView for the project structure

    public EditorHandler(EditorScreen editorScreen, Path projectDirectory, TreeView<Path> projectTreeView) {
        this.editorScreen = editorScreen;
        this.projectDirectory = projectDirectory;
        this.projectTreeView = projectTreeView;
        System.out.println("EditorHandler initialized with projectTree: " + projectTreeView);
        refreshProjectTree(); // Initialize the tree with the current project directory
        handleTabCloseEvent(); // Handle tab close events
    }

    // ============================== //
    // === Handle "New" File Click === //
    // ============================== //
    public void handleNewFile(Stage stage) {
        CodeArea newCodeArea = createCodeArea();
        ScrollPane newScrollPane = createScrollPane(newCodeArea);

        Tab newTab = new Tab("Untitled");
        newTab.setContent(newScrollPane);

        editorScreen.getEditorTabPane().getTabs().add(newTab);
        editorScreen.getEditorTabPane().getSelectionModel().select(newTab);
    }

    // ============================== //
    // === Handle "Open" File Click == //
    // ============================== //
    public void handleOpenFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");

        // Set the initial directory to the current project directory
        fileChooser.setInitialDirectory(projectDirectory.toFile());

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));

                CodeArea newCodeArea = createCodeArea();
                newCodeArea.replaceText(content);

                ScrollPane newScrollPane = createScrollPane(newCodeArea);

                Tab newTab = new Tab(selectedFile.getName());
                newTab.setContent(newScrollPane);

                editorScreen.getEditorTabPane().getTabs().add(newTab);
                editorScreen.getEditorTabPane().getSelectionModel().select(newTab);

                tabFileMap.put(newTab, selectedFile);

                newCodeArea.requestFocus();
            } catch (IOException e) {
                showError("Failed to open file: " + e.getMessage());
            }
        }
    }

    // ============================== //
    // === Handle "Save" File Click == //
    // ============================== //
    public void handleSaveFile(Stage stage) {
        Tab currentTab = editorScreen.getEditorTabPane().getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            File file = tabFileMap.get(currentTab);
            if (file != null) {
                saveCurrentTabContent(file, currentTab);
                refreshProjectTree(); // Refresh the tree after saving
                addFileToProjectTree(file); // Directly add the saved file to the tree
            } else {
                handleSaveAsFile(stage); // If no file is linked, prompt Save As
            }
        }
    }

    // ================================ //
    // == Handle "Save As" File Click == //
    // ================================ //
    public void handleSaveAsFile(Stage stage) {
        Tab currentTab = editorScreen.getEditorTabPane().getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save As");

            // Set the initial directory to the current project directory
            fileChooser.setInitialDirectory(projectDirectory.toFile());

            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                saveCurrentTabContent(file, currentTab);
                tabFileMap.put(currentTab, file);
                currentTab.setText(file.getName());
                addFileToProjectTree(file); // Add the new file to the project tree
                refreshProjectTree(); // Refresh the tree after adding the file
            }
        }
    }

    // ============================== //
    // === Update Project Tree === //
    // ============================== //
    private void addFileToProjectTree(File file) {
        if (projectTreeView != null && projectDirectory != null) {
            TreeItem<Path> root = projectTreeView.getRoot();
            Path filePath = file.toPath();
            TreeItem<Path> newItem = new TreeItem<>(filePath);

            // Ensure that the parent directory is in the tree before adding the file
            TreeItem<Path> parentDirectoryItem = findOrCreateParentDirectoryItem(root, filePath.getParent());

            // Add the new file to the correct parent
            if (parentDirectoryItem != null) {
                parentDirectoryItem.getChildren().add(newItem);
                projectTreeView.refresh(); // Refresh the tree immediately after adding a new file
            } else {
                System.err.println("Unable to add file: " + filePath);
            }
        }
    }

    // Find or create a parent directory item
    private TreeItem<Path> findOrCreateParentDirectoryItem(TreeItem<Path> rootItem, Path parentPath) {
        for (TreeItem<Path> childItem : rootItem.getChildren()) {
            if (childItem.getValue().equals(parentPath)) {
                return childItem;
            } else if (Files.isDirectory(childItem.getValue())) {
                TreeItem<Path> foundItem = findOrCreateParentDirectoryItem(childItem, parentPath);
                if (foundItem != null) {
                    return foundItem;
                }
            }
        }
        // If parent doesn't exist, create it
        TreeItem<Path> newParentItem = new TreeItem<>(parentPath);
        rootItem.getChildren().add(newParentItem);
        return newParentItem;
    }

    // ============================== //
    // === Update Project Directory === //
    // ============================== //
    public void updateProjectDirectory(Path newProjectDirectory) {
        this.projectDirectory = newProjectDirectory;
        refreshProjectTree(); // Refresh the tree when the directory is updated
    }

    public void refreshProjectTree() {
        if (projectDirectory == null || !Files.exists(projectDirectory)) {
            showError("Invalid project directory.");
            return;
        }

        TreeItem<Path> root = createTreeItem(projectDirectory);
        if (projectTreeView != null) {
            projectTreeView.setRoot(root);
        } else {
            // Handle the case when projectTree is null
            System.err.println("Project Tree is not initialized yet.");
        }
        root.setExpanded(true); // Optionally expand the root
        projectTreeView.refresh(); // Ensure the tree is refreshed immediately after a change
    }

    private TreeItem<Path> createTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        if (Files.isDirectory(path)) {
            addFilesToTree(item, path);
        }
        return item;
    }

    private void addFilesToTree(TreeItem<Path> parentItem, Path folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                TreeItem<Path> item = new TreeItem<>(entry);
                parentItem.getChildren().add(item);
                if (Files.isDirectory(entry)) {
                    addFilesToTree(item, entry);
                }
            }
        } catch (IOException e) {
            showError("Error loading project files: " + e.getMessage());
        }
    }

    // ============================== //
    // === Handle Tab Close Event === //
    // ============================== //
    private void handleTabCloseEvent() {
        editorScreen.getEditorTabPane().getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab tab : change.getRemoved()) {
                        tabFileMap.remove(tab);
                    }
                }
            }
        });
    }

    // ============================== //
    // === Helper Methods === //
    // ============================== //
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private CodeArea createCodeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");
        return codeArea;
    }

    private ScrollPane createScrollPane(CodeArea codeArea) {
        ScrollPane scrollPane = new ScrollPane(codeArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        codeArea.prefWidthProperty().bind(scrollPane.widthProperty());
        codeArea.prefHeightProperty().bind(scrollPane.heightProperty());
        return scrollPane;
    }

    private void saveCurrentTabContent(File file, Tab tab) {
        ScrollPane scrollPane = (ScrollPane) tab.getContent();
        CodeArea codeArea = (CodeArea) scrollPane.getContent();
        String content = codeArea.getText();

        try {
            Files.write(file.toPath(), content.getBytes());
        } catch (IOException e) {
            showError("Failed to save file: " + e.getMessage());
        }
    }
}
