package com.DevScribe.ui.components;

import com.DevScribe.ui.screen.EditorScreen;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class EditorHandler {

    private final EditorScreen editorScreen;
    private final Map<Tab, File> tabFileMap = new HashMap<>();

    public EditorHandler(EditorScreen editorScreen) {
        this.editorScreen = editorScreen;
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
            } else {
                handleSaveAsFile(stage);
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

            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                saveCurrentTabContent(file, currentTab);
                tabFileMap.put(currentTab, file);
                currentTab.setText(file.getName());
            }
        }
    }

    // ================================ //
    // ===== Helper Methods ========== //
    // ================================ //
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
