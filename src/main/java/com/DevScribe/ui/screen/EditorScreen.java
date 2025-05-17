package com.DevScribe.ui.screen;

import com.DevScribe.editor.highlighting.CHighlighter;
import com.DevScribe.editor.highlighting.JavaHighlighter;
import com.DevScribe.editor.highlighting.LanguageHighlighter;
import com.DevScribe.editor.highlighting.PythonHighlighter;
import com.DevScribe.model.Language;
import com.DevScribe.ui.components.EditorHandler;
import com.DevScribe.ui.dialogs.Terminal;
import com.DevScribe.utils.PathValidator;
import com.DevScribe.utils.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorScreen {
    private double xOffset = 0;
    private double yOffset = 0;

    private BorderPane root;
    private TabPane editorTabPane;
    private TreeView<Path> projectTree;
    private Terminal terminal;
    private EditorHandler editorHandler;
    private Path projectPath;

    private final Map<Language, LanguageHighlighter> highlighterMap = Map.of(
            Language.JAVA, new JavaHighlighter(),
            Language.PYTHON, new PythonHighlighter(),
            Language.C, new CHighlighter()
    );

    // Track language for currently opened file (default Java)
    private Language currentLanguage = Language.JAVA;

    public EditorScreen() {
        terminal = new Terminal();
    }

    public void start(Stage stage, Path projectPath) {
        this.projectPath = projectPath;

        if (!PathValidator.validateProjectPath(projectPath)) {
            showErrorDialog("Invalid Project", "Project path is invalid or inaccessible.");
            return;
        }

        root = new BorderPane();

        stage.initStyle(StageStyle.UNDECORATED);

        // Initialize project tree and editor handler
        projectTree = new TreeView<>(createTreeItem(projectPath));
        projectTree.setShowRoot(true);
        projectTree.setCellFactory(param -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getFileName().toString());
                }
            }
        });

        projectTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Path> selectedItem = projectTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null && Files.isRegularFile(selectedItem.getValue())) {
                    openFileInEditor(selectedItem.getValue());
                }
            }
        });

        editorHandler = new EditorHandler(this, projectPath, projectTree);

        setupEditorArea();

        root.setTop(createHeader(stage));
        root.setLeft(createLeftNav());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1400, 750);
        scene.getStylesheets().add(getClass().getResource("/css/editor.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("Ctrl+N"),
                () -> editorHandler.handleNewFile(stage)
        );

        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("Ctrl+O"),
                () -> editorHandler.handleOpenFile(stage)
        );

        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("Ctrl+S"),
                () -> editorHandler.handleSaveFile(stage)
        );

        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.keyCombination("Ctrl+Shift+S"),
                () -> editorHandler.handleSaveAsFile(stage)
        );
    }

    public TabPane getEditorTabPane() {
        if (editorTabPane == null) {
            setupEditorArea();
        }
        return editorTabPane;
    }

    private void setupEditorArea() {
        editorTabPane = new TabPane();
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        editorTabPane.setTabMinWidth(100);
        editorTabPane.setStyle("-fx-background-color: #1e1e24;");

        terminal.setVisible(false);
        terminal.setManaged(false);
        terminal.setPrefHeight(150);

        VBox editorTerminalContainer = new VBox(editorTabPane, terminal);
        VBox.setVgrow(editorTabPane, Priority.ALWAYS);
        VBox.setVgrow(terminal, Priority.SOMETIMES);

        root.setCenter(editorTerminalContainer);
    }


    private VBox createLeftNav() {
        VBox leftNav = new VBox();
        leftNav.getStyleClass().add("left-nav");

        VBox projectToolbar = new VBox();
        projectToolbar.getStyleClass().add("project-toolbar");

        Button toggleBtn = new Button("Folder");
        toggleBtn.getStyleClass().add("folder-toggle-btn");
        projectToolbar.getChildren().add(toggleBtn);

        VBox directory = new VBox();
        Label directoryLabel = new Label("Project");
        directoryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-pref-width: 250px; -fx-background-color: #23232B");

        directory.getChildren().add(directoryLabel);
        directory.getChildren().add(projectTree);

        AtomicBoolean isVisible = new AtomicBoolean(true);
        toggleBtn.setOnAction(e -> {
            isVisible.set(!isVisible.get());
            directory.setVisible(isVisible.get());
            directory.setManaged(isVisible.get());

            FontIcon icon = (FontIcon) toggleBtn.getGraphic();
            if (icon != null) {
                icon.setIconCode(isVisible.get() ? MaterialDesignF.FOLDER : MaterialDesignF.FOLDER_OPEN);
            }
        });

        HBox container = new HBox(projectToolbar, directory);
        leftNav.getChildren().add(container);
        return leftNav;
    }

    private HBox createHeader(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(16);
        logoView.setPreserveRatio(true);

        Label title = new Label("DevScribe");
        title.getStyleClass().add("header-title");

        HBox logoTitleGroup = new HBox(3, logoView, title);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);

        HBox menuBar = new HBox(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        MenuButton fileMenu = new MenuButton("File");
        MenuItem newFile = new MenuItem("New");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem saveAsFile = new MenuItem("Save As");
        MenuItem exit = new MenuItem("Exit");
        fileMenu.getItems().addAll(newFile, openFile, saveFile, saveAsFile, new SeparatorMenuItem(), exit);

        MenuButton editMenu = new MenuButton("Edit");
        MenuItem undo = new MenuItem("Undo");
        MenuItem redo = new MenuItem("Redo");
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        editMenu.getItems().addAll(undo, redo, new SeparatorMenuItem(), cut, copy, paste);

        MenuButton viewMenu = new MenuButton("View");
        CheckMenuItem wordWrap = new CheckMenuItem("Word Wrap");
        CheckMenuItem toggleTerminal = new CheckMenuItem("Show Terminal");
        MenuItem zoomIn = new MenuItem("Zoom In");
        MenuItem zoomOut = new MenuItem("Zoom Out");
        viewMenu.getItems().addAll(toggleTerminal, wordWrap, new SeparatorMenuItem(), zoomIn, zoomOut);

        toggleTerminal.setOnAction(event -> {
            boolean visible = toggleTerminal.isSelected();
            terminal.setVisible(visible);
            terminal.setManaged(visible);
        });

        menuBar.getChildren().addAll(fileMenu, editMenu, viewMenu);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button runButton = createTitleBarButton("\u25B6", () -> {
            Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {
                ScrollPane scrollPane = (ScrollPane) selectedTab.getContent();
                CodeArea area = (CodeArea) scrollPane.getContent();
                Path filePath = findFileInProject(selectedTab.getText());

                if (filePath != null) {
                    terminal.showTerminal(filePath, area.getText());
                    terminal.setVisible(true);
                    terminal.setManaged(true);
                } else {
                    System.out.println("File path not found.");
                }
            } else {
                System.out.println("No file selected.");
            }
        });
        runButton.getStyleClass().add("run-button");

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        HBox buttonContainer = new HBox(8, runButton, minimizeButton, maximizeButton, closeButton);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        titleBar.getChildren().addAll(logoTitleGroup, menuBar, spacer, buttonContainer);

        // Dragging window
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // File menu actions
        newFile.setOnAction(e -> editorHandler.handleNewFile(stage));
        openFile.setOnAction(e -> editorHandler.handleOpenFile(stage));
        saveFile.setOnAction(e -> editorHandler.handleSaveFile(stage));
        saveAsFile.setOnAction(e -> editorHandler.handleSaveAsFile(stage));
        exit.setOnAction(e -> ScreenManager.switchToLauncher(stage));

        return titleBar;
    }

    private Button createTitleBarButton(String symbol, Runnable action) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add("title-bar-button");
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Path findFileInProject(String fileName) {
        try {
            return Files.walk(projectPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private CodeArea openFileInEditor(Path filePath) {
        if (editorTabPane == null) {
            setupEditorArea();
        }

        // If tab already open, select it
        for (Tab tab : editorTabPane.getTabs()) {
            if (tab.getText().equals(filePath.getFileName().toString())) {
                editorTabPane.getSelectionModel().select(tab);
                return null;
            }
        }

        String content = "";
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            showErrorDialog("File Open Error", "Could not open file: " + e.getMessage());
            return null;
        }

        CodeArea codeArea = new CodeArea(content);
        codeArea.setWrapText(true);
        codeArea.minHeight(550);

        Tab tab = new Tab(filePath.getFileName().toString());
        ScrollPane scrollPane = new ScrollPane(codeArea);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);

        Language language = determineLanguageFromExtension(filePath);
        applySyntaxHighlighting(codeArea, language);

        final String originalContent = content;

        // Add listener for changes in codeArea text
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.equals(originalContent)) {
                // Mark tab as unsaved by adding "*" prefix if not already there
                if (!tab.getText().startsWith("*")) {
                    tab.setText("*" + filePath.getFileName().toString());
                }
            } else {
                // Remove unsaved indicator if text matches original
                if (tab.getText().startsWith("*")) {
                    tab.setText(filePath.getFileName().toString());
                }
            }
        });


        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
        return codeArea;
    }


    private Language determineLanguageFromExtension(Path filePath) {
        String filename = filePath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".java")) return Language.JAVA;
        else if (filename.endsWith(".py")) return Language.PYTHON;
        else if (filename.endsWith(".c") || filename.endsWith(".h")) return Language.C;
        else return Language.JAVA; // default fallback
    }

    private void applySyntaxHighlighting(CodeArea codeArea, Language language) {
        LanguageHighlighter highlighter = highlighterMap.get(language);
        if (highlighter == null) return;

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // only when text changes
                .successionEnds(Duration.ofMillis(500))
                .subscribe(ignore -> {
                    int caretPosition = codeArea.getCaretPosition();
                    codeArea.setStyleSpans(0, highlighter.computeHighlighting(codeArea.getText()));
                    codeArea.moveTo(caretPosition);
                });

        // Initial highlighting
        codeArea.setStyleSpans(0, highlighter.computeHighlighting(codeArea.getText()));
    }

    private TreeItem<Path> createTreeItem(Path path) {
        TreeItem<Path> rootItem = new TreeItem<>(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    rootItem.getChildren().add(createTreeItem(entry));
                } else {
                    rootItem.getChildren().add(new TreeItem<>(entry));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rootItem;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");

        Label statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
