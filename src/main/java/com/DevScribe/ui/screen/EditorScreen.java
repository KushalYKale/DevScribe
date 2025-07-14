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
import javafx.geometry.Orientation;
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
    private boolean isDarkTheme = true;

    private final Map<Language, LanguageHighlighter> highlighterMap = Map.of(
            Language.JAVA, new JavaHighlighter(),
            Language.PYTHON, new PythonHighlighter(),
            Language.C, new CHighlighter()
    );

    private Language currentLanguage = Language.JAVA;

    public EditorScreen() {
        terminal = new Terminal();
    }

    public void start(Stage stage, Path projectPath,boolean isDarkTheme){
        this.projectPath = projectPath;
        this.isDarkTheme = isDarkTheme;

        System.out.println("Before initStyle");
        stage.initStyle(StageStyle.UNDECORATED);
        System.out.println("After initStyle");

        if (!PathValidator.validateProjectPath(projectPath)) {
            showErrorDialog("Invalid Project", "Project path is invalid or inaccessible.");
            return;
        }

        root = new BorderPane();

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
        updateTheme(scene);
        System.out.println("Before show");
        stage.show();
        System.out.println("After show");


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

    private void updateTheme(Scene scene) {
        System.out.println("Updating theme: isDarkTheme = " + isDarkTheme);
        scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
        scene.getRoot().getStyleClass().add(isDarkTheme ? "dark-theme" : "light-theme");
    }

    private void setupEditorArea() {
        editorTabPane = new TabPane();
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        editorTabPane.setTabMinWidth(100);
        editorTabPane.getStyleClass().add("editor-tab-pane");

        terminal.setPrefHeight(150);

        SplitPane editorTerminalSplitPane = new SplitPane();
        editorTerminalSplitPane.setOrientation(Orientation.VERTICAL);

        editorTerminalSplitPane.getItems().addAll(editorTabPane, terminal);

        editorTerminalSplitPane.setDividerPositions(0.75);

        // Hide terminal initially, remove it from split pane so editor expands fully
        terminal.setVisible(false);
        terminal.setManaged(false);
        editorTerminalSplitPane.getItems().remove(terminal);

        root.setCenter(editorTerminalSplitPane);
    }


    private VBox createLeftNav() {
        VBox leftNav = new VBox();
        leftNav.getStyleClass().add("left-nav");

        VBox projectToolbar = new VBox();
        projectToolbar.getStyleClass().add("project-toolbar");

        ImageView folderIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/folder.png")));
        folderIcon.setFitWidth(16);
        folderIcon.setFitHeight(16);

        Button addFolderBtn = new Button();
        addFolderBtn.setGraphic(folderIcon);
        addFolderBtn.setTooltip(new Tooltip("Add New Folder"));

        Button toggleBtn = new Button();
        toggleBtn.setGraphic(addFolderBtn);
        toggleBtn.getStyleClass().add("folder-toggle-btn");
        projectToolbar.getChildren().add(toggleBtn);

        VBox directory = new VBox();
        Label directoryLabel = new Label("Project");
        directoryLabel.getStyleClass().add("directory-label");

        directory.getChildren().add(directoryLabel);
        directory.getChildren().addAll(projectTree);

        AtomicBoolean isVisible = new AtomicBoolean(true);
        toggleBtn.setOnAction(e -> {
            isVisible.set(!isVisible.get());
            directory.setVisible(isVisible.get());
            directory.setManaged(isVisible.get());

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

        undo.setOnAction(e -> {
            CodeArea area = getCurrentCodeArea();
            if (area != null) area.undo();
        });
        redo.setOnAction(e -> {
            CodeArea area = getCurrentCodeArea();
            if (area != null) area.redo();
        });
        cut.setOnAction(e -> {
            CodeArea area = getCurrentCodeArea();
            if (area != null) area.cut();
        });
        copy.setOnAction(e -> {
            CodeArea area = getCurrentCodeArea();
            if (area != null) area.copy();
        });
        paste.setOnAction(e -> {
            CodeArea area = getCurrentCodeArea();
            if (area != null) area.paste();
        });

        MenuButton viewMenu = new MenuButton("View");
        CheckMenuItem wordWrap = new CheckMenuItem("Word Wrap");
        CheckMenuItem toggleTerminal = new CheckMenuItem("Show Terminal");
        CheckMenuItem toggleTheme = new CheckMenuItem("Dark Mode");
        MenuItem zoomIn = new MenuItem("Zoom In");
        MenuItem zoomOut = new MenuItem("Zoom Out");
        viewMenu.getItems().addAll(toggleTerminal, wordWrap, toggleTheme, new SeparatorMenuItem(), zoomIn, zoomOut);

        toggleTerminal.setOnAction(event -> {
            boolean visible = toggleTerminal.isSelected();

            SplitPane splitPane = (SplitPane) root.getCenter();  // get your split pane reference

            if (visible) {
                if (!splitPane.getItems().contains(terminal)) {
                    splitPane.getItems().add(terminal);
                }
                terminal.setVisible(true);
                terminal.setManaged(true);
                splitPane.setDividerPositions(0.75);
            } else {
                terminal.setVisible(false);
                terminal.setManaged(false);
                splitPane.getItems().remove(terminal);
            }
        });


        toggleTheme.setOnAction(event -> {
            isDarkTheme = toggleTheme.isSelected();
            updateTheme(stage.getScene());
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
                    SplitPane splitPane = (SplitPane) root.getCenter();
                    if (!splitPane.getItems().contains(terminal)) {
                        splitPane.getItems().add(terminal);
                        splitPane.setDividerPositions(0.75);
                    }
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
        Tooltip tooltip = new Tooltip("Click to run the code");
        tooltip.setShowDelay(javafx.util.Duration.millis(100));
        runButton.setTooltip(tooltip);
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
            if (tab.getText().replace("*", "").equals(filePath.getFileName().toString())) {
                editorTabPane.getSelectionModel().select(tab);
                return null;
            }
        }

        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            showErrorDialog("File Open Error", "Could not open file: " + e.getMessage());
            return null;
        }

        CodeArea codeArea = new CodeArea(content);
        codeArea.setWrapText(true);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        ScrollPane scrollPane = new ScrollPane(codeArea);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        Tab tab = new Tab(filePath.getFileName().toString(), scrollPane);
        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);

        Language language = determineLanguageFromExtension(filePath);
        applySyntaxHighlighting(codeArea, language);

        // Handle unsaved file indication
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.equals(content)) {
                if (!tab.getText().startsWith("*")) {
                    tab.setText("*" + tab.getText());
                }
            } else {
                if (tab.getText().startsWith("*")) {
                    tab.setText(tab.getText().substring(1));
                }
            }
        });

        // Set tooltip to show full path
        Tooltip tooltip = new Tooltip(filePath.toString());
        tooltip.setShowDelay(javafx.util.Duration.millis(100));
        Tooltip.install(tab.getGraphic(), tooltip);
        tab.setTooltip(tooltip);

        // Close tab and cleanup
        tab.setOnCloseRequest(event -> {
            // Optionally prompt to save if unsaved
            if (tab.getText().startsWith("*")) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes in " + filePath.getFileName());
                alert.setContentText("Do you want to save your changes before closing?");
                ButtonType save = new ButtonType("Save");
                ButtonType dontSave = new ButtonType("Don't Save");
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(save, dontSave, cancel);

                alert.showAndWait().ifPresent(response -> {
                    if (response == save) {
                        try {
                            Files.writeString(filePath, codeArea.getText());
                        } catch (IOException e) {
                            showErrorDialog("Save Error", "Failed to save file.");
                        }
                    } else if (response == cancel) {
                        event.consume();
                    }
                });
            }
        });

        return codeArea;
    }

    private CodeArea getCurrentCodeArea() {
        if (editorTabPane == null) return null;
        Tab tab = editorTabPane.getSelectionModel().getSelectedItem();
        if (tab == null) return null;

        // your tab holds a ScrollPane whose content is the CodeArea
        ScrollPane scroll = (ScrollPane) tab.getContent();
        return (CodeArea) scroll.getContent();
    }



    private Language determineLanguageFromExtension(Path filePath) {
        String filename = filePath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".java")) return Language.JAVA;
        else if (filename.endsWith(".py")) return Language.PYTHON;
        else if (filename.endsWith(".c") || filename.endsWith(".h")) return Language.C;
        else return Language.JAVA;
    }

    private void applySyntaxHighlighting(CodeArea codeArea, Language language) {
        LanguageHighlighter highlighter = highlighterMap.get(language);
        if (highlighter == null) return;

        // Use a duration to debounce syntax highlighting updates
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> {
                    String text = codeArea.getText();
                    var styledSpans = highlighter.computeHighlighting(text);
                    codeArea.setStyleSpans(0, styledSpans);
                });

        // Optional: apply initial highlighting
        String initialText = codeArea.getText();
        codeArea.setStyleSpans(0, highlighter.computeHighlighting(initialText));
    }


    private TreeItem<Path> createTreeItem(Path path) {
        TreeItem<Path> treeItem = new TreeItem<>(path);
        if (Files.isDirectory(path)) {
            // Add a dummy child to show expand icon
            treeItem.getChildren().add(new TreeItem<>(null));
            // Add expansion listener to load children lazily
            treeItem.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && treeItem.getChildren().size() == 1 && treeItem.getChildren().get(0).getValue() == null) {
                    treeItem.getChildren().clear();
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                        for (Path entry : stream) {
                            treeItem.getChildren().add(createTreeItem(entry));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return treeItem;
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
