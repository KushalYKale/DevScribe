package com.DevScribe.ui.components;

import com.DevScribe.ui.screen.EditorScreen;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class EditorHandler {

    private final EditorScreen editorScreen;
    private final Map<Tab, File> tabFileMap = new HashMap<>();
    private final Map<Tab, Boolean> unsavedChangesMap = new HashMap<>();
    private Path projectDirectory;
    private TreeView<Path> projectTreeView;
    CodeArea codeArea = new CodeArea();

    public EditorHandler(EditorScreen editorScreen, Path projectDirectory, TreeView<Path> projectTreeView) {
        this.editorScreen = editorScreen;
        this.projectDirectory = projectDirectory;
        this.projectTreeView = projectTreeView;
        refreshProjectTree();
        setupProjectTreeContextMenu();
        handleTabCloseEvent();
    }

    // ===================== File & Tab Handling =====================

    public void handleNewFile(Stage stage) {
        Tab newTab = new Tab("Untitled");
        CodeArea codeArea = createCodeArea(newTab);
        ScrollPane scrollPane = createScrollPane(codeArea);
        newTab.setContent(scrollPane);

        editorScreen.getEditorTabPane().getTabs().add(newTab);
        editorScreen.getEditorTabPane().getSelectionModel().select(newTab);
        unsavedChangesMap.put(newTab, false);
        tabFileMap.put(newTab, null);
    }

    public void handleOpenFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.setInitialDirectory(projectDirectory.toFile());

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));
                Tab newTab = new Tab(selectedFile.getName());
                CodeArea codeArea = createCodeArea(newTab);
                codeArea.replaceText(content);

                ScrollPane scrollPane = createScrollPane(codeArea);
                newTab.setContent(scrollPane);

                editorScreen.getEditorTabPane().getTabs().add(newTab);
                editorScreen.getEditorTabPane().getSelectionModel().select(newTab);

                tabFileMap.put(newTab, selectedFile);
                unsavedChangesMap.put(newTab, false);

                codeArea.requestFocus();
            } catch (IOException e) {
                showError("Failed to open file: " + e.getMessage());
            }
        }
    }

    public void handleSaveFile(Stage stage) {
        if (stage == null) {
            stage = getStage();
            if (stage == null) {
                showError("Cannot save file: No application window found.");
                return;
            }
        }

        Tab currentTab = editorScreen.getEditorTabPane().getSelectionModel().getSelectedItem();
        if (currentTab == null) return;

        File file = tabFileMap.get(currentTab);
        System.out.println("handleSaveFile: currentTab=" + currentTab.getText() + ", file=" + file);

        if (file != null) {
            saveCurrentTabContent(file, currentTab);
            refreshProjectTree();
        } else {
            handleSaveAsFile(stage);
        }
    }

    // Save As dialog to save file content at chosen location
    public void handleSaveAsFile(Stage stage) {
        if (stage == null) {
            stage = getStage();
            if (stage == null) {
                showError("Cannot save file: No application window found.");
                return;
            }
        }

        Tab currentTab = editorScreen.getEditorTabPane().getSelectionModel().getSelectedItem();
        if (currentTab == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        if (projectDirectory != null && Files.exists(projectDirectory)) {
            fileChooser.setInitialDirectory(projectDirectory.toFile());
        }

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            saveCurrentTabContent(file, currentTab);
            tabFileMap.put(currentTab, file);
            updateTabTitle(currentTab, file.getName());
            refreshProjectTree();
        }
    }

    private void saveCurrentTabContent(File file, Tab tab) {
        ScrollPane scrollPane = (ScrollPane) tab.getContent();
        CodeArea codeArea = (CodeArea) scrollPane.getContent();
        String content = codeArea.getText();

        try {
            Path parentDir = file.toPath().getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir); // Ensure parent directories exist
            }
            Files.write(file.toPath(), content.getBytes());
            markTabAsSaved(tab);
            updateTabTitle(tab, file.getName());
        } catch (IOException e) {
            showError("Failed to save file: " + e.getMessage());
        }
    }

    private CodeArea createCodeArea(Tab tab) {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!unsavedChangesMap.getOrDefault(tab, false)) {
                unsavedChangesMap.put(tab, true);
                updateTabTitle(tab, tab.getText().replace("*", ""));
            }
        });

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

    // ===================== Project Tree Handling =====================

    public void refreshProjectTree() {
        if (projectDirectory == null || !Files.exists(projectDirectory)) {
            showError("Invalid project directory.");
            return;
        }
        TreeItem<Path> root = createTreeItem(projectDirectory);
        if (projectTreeView != null) {
            projectTreeView.setRoot(root);
            root.setExpanded(true);
            projectTreeView.refresh();
        }
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

    // ===================== Context Menu with New, Rename, Delete =====================

    public void setupProjectTreeContextMenu() {
        projectTreeView.setCellFactory(tv -> {
            TreeCell<Path> cell = new TreeCell<>() {
                @Override
                protected void updateItem(Path item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getFileName().toString());
                }
            };

            ContextMenu contextMenu = new ContextMenu();

            MenuItem newFile = new MenuItem("New File");
            newFile.setOnAction(e -> {
                Path selectedDir = getSelectedDirectory(cell);
                if (selectedDir != null) {
                    createNewFile(selectedDir);
                }
            });

            MenuItem newFolder = new MenuItem("New Folder");
            newFolder.setOnAction(e -> {
                Path selectedDir = getSelectedDirectory(cell);
                if (selectedDir != null) {
                    createNewFolder(selectedDir);
                }
            });

            MenuItem rename = new MenuItem("Rename");
            rename.setOnAction(e -> {
                Path selected = cell.getItem();
                if (selected != null) {
                    renameItem(selected);
                }
            });

            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(e -> {
                Path selected = cell.getItem();
                if (selected != null) {
                    deleteItem(selected);
                }
            });

            contextMenu.getItems().addAll(newFile, newFolder, rename, delete);

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                cell.setContextMenu(isNowEmpty ? null : contextMenu);
            });

            return cell;
        });
    }

    private Path getSelectedDirectory(TreeCell<Path> cell) {
        Path path = cell.getItem();
        if (path != null && Files.isDirectory(path)) {
            return path;
        } else if (path != null) {
            return path.getParent();
        }
        return null;
    }

    private void createNewFile(Path parentDir) {
        TextInputDialog dialog = new TextInputDialog("NewFile.txt");
        dialog.setTitle("New File");
        dialog.setHeaderText("Create a New File");
        dialog.setContentText("Enter file name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(filename -> {
            Path newFilePath = parentDir.resolve(filename);
            try {
                Files.createFile(newFilePath);
                refreshProjectTree();
            } catch (IOException e) {
                showError("Failed to create file: " + e.getMessage());
            }
        });
    }

    private void createNewFolder(Path parentDir) {
        TextInputDialog dialog = new TextInputDialog("NewFolder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a New Folder");
        dialog.setContentText("Enter folder name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(folderName -> {
            Path newFolderPath = parentDir.resolve(folderName);
            try {
                Files.createDirectory(newFolderPath);
                refreshProjectTree();
            } catch (IOException e) {
                showError("Failed to create folder: " + e.getMessage());
            }
        });
    }

    private void renameItem(Path oldPath) {
        TextInputDialog dialog = new TextInputDialog(oldPath.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename File or Folder");
        dialog.setContentText("Enter new name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            Path newPath = oldPath.resolveSibling(newName);
            try {
                Files.move(oldPath, newPath);
                // Update tabFileMap for open tabs with this file
                for (Map.Entry<Tab, File> entry : tabFileMap.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().toPath().equals(oldPath)) {
                        File newFile = newPath.toFile();
                        entry.setValue(newFile);
                        Tab tab = entry.getKey();
                        updateTabTitle(tab, newFile.getName());
                    }
                }
                refreshProjectTree();
            } catch (IOException e) {
                showError("Failed to rename: " + e.getMessage());
            }
        });
    }

    private void deleteItem(Path path) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete " + path.getFileName() + "?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (Files.isDirectory(path)) {
                    Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ex) {
                                    showError("Failed to delete: " + ex.getMessage());
                                }
                            });
                } else {
                    Files.deleteIfExists(path);
                }
                refreshProjectTree();
            } catch (IOException e) {
                showError("Failed to delete: " + e.getMessage());
            }
        }
    }

    // ===================== Utility =====================

    private void handleTabCloseEvent() {
        editorScreen.getEditorTabPane().getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab tab : change.getRemoved()) {
                        tabFileMap.remove(tab);
                        unsavedChangesMap.remove(tab);
                    }
                } else if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        tab.setOnCloseRequest(event -> {
                            boolean hasUnsavedChanges = unsavedChangesMap.getOrDefault(tab, false);

                            // If there are unsaved changes for either saved or new file, prompt user
                            if (hasUnsavedChanges) {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Unsaved Changes");
                                alert.setHeaderText("You have unsaved changes.");
                                alert.setContentText("Do you want to save before closing?");

                                ButtonType save = new ButtonType("Save");
                                ButtonType discard = new ButtonType("Discard");
                                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                                alert.getButtonTypes().setAll(save, discard, cancel);

                                Optional<ButtonType> result = alert.showAndWait();
                                if (result.isPresent()) {
                                    if (result.get() == save) {
                                        Stage stage = getStage();
                                        if (stage == null) {
                                            showError("Cannot save file: No application window found.");
                                            event.consume();
                                            return;
                                        }
                                        handleSaveFile(stage);

                                        // After saving, still check if tab still has unsaved changes (in case save failed)
                                        if (unsavedChangesMap.getOrDefault(tab, false)) {
                                            event.consume(); // prevent closing if still unsaved
                                        }
                                    } else if (result.get() == cancel) {
                                        event.consume(); // cancel tab closing
                                    }
                                } else {
                                    event.consume(); // no choice made, cancel close
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private Stage getStage() {
        if (editorScreen != null && editorScreen.getEditorTabPane() != null &&
                editorScreen.getEditorTabPane().getScene() != null) {
            return (Stage) editorScreen.getEditorTabPane().getScene().getWindow();
        }
        return null;
    }


    private void markTabAsSaved(Tab tab) {
        unsavedChangesMap.put(tab, false);
        updateTabTitle(tab, tab.getText().replace("*", ""));
    }

    private void updateTabTitle(Tab tab, String title) {
        if (unsavedChangesMap.getOrDefault(tab, false)) {
            if (!title.endsWith("*")) {
                tab.setText(title + "*");
            }
        } else {
            tab.setText(title);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
