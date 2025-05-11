package com.DevScribe.ui.screen;

import com.DevScribe.ui.components.EditorHandler;
import com.DevScribe.utils.PathValidator;
import com.DevScribe.utils.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorScreen {
    private double xOffset = 0;
    private double yOffset = 0;
    private BorderPane root;
    private TabPane editorTabPane;
    private ScrollPane scrollPane;
    private CodeArea codeArea;
    private EditorHandler editorHandler;
    public TreeView<Path> projectTree;

    private Path projectPath;

    public void start(Stage stage, Path projectPath) {
        this.projectPath = projectPath;

        if (!PathValidator.validateProjectPath(projectPath)) {
            return;
        }
        root = new BorderPane();

        System.out.println("Opening editor for project: " + projectPath);
        stage.initStyle(StageStyle.UNDECORATED);


        if (!PathValidator.validateProjectPath(projectPath)) {
            return;
        }

        projectTree = new TreeView<>(createTreeItem(projectPath));  // Initialize projectTree here
        projectTree.setShowRoot(true);
        projectTree.setCellFactory(param -> new TreeCell<Path>() {
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

        editorHandler = new EditorHandler(this, projectPath, projectTree);

        setupEditorArea();
        root.setTop(createHeader(stage));
        setupStatusBar();
        root.setLeft(leftNav());
        loadProjectFiles();

        Scene scene = new Scene(root, 1400, 750);
        scene.getStylesheets().add(getClass().getResource("/css/editor.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public TabPane getEditorTabPane() {
        if (editorTabPane == null) {
            editorTabPane = new TabPane();
        }
        return editorTabPane;
    }

    private boolean isRunningFromJar() {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        return path.endsWith(".jar");
    }

    private void loadProjectFiles() {
        Path defaultFilePath;

        // If running from JAR, use the default path for the project
        if (isRunningFromJar()) {
            defaultFilePath = Paths.get(System.getProperty("user.home"), "DevScribe", "projects", "src", "Main.java");
        } else {
            defaultFilePath = projectPath.resolve("src").resolve("Main.java");
        }

        // If a default file exists, load it into the editor
        if (Files.exists(defaultFilePath)) {
            try {
                String content = Files.readString(defaultFilePath);
                codeArea.replaceText(content);
            } catch (IOException e) {
                showErrorDialog("Error", "Failed to read the file.");
            }
        } else {
            // If no default file, show a placeholder message
            codeArea.replaceText("// No files found. Start coding...");
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        MenuItem zoomIn = new MenuItem("Zoom In");
        MenuItem zoomOut = new MenuItem("Zoom Out");
        viewMenu.getItems().addAll(wordWrap, new SeparatorMenuItem(), zoomIn, zoomOut);

        menuBar.getChildren().addAll(fileMenu, editMenu, viewMenu);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button runButton = createTitleBarButton("\u25B6", () -> System.out.println("Run button clicked"));
        runButton.getStyleClass().add("run-button");

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        HBox buttonContainer = new HBox(8, runButton, minimizeButton, maximizeButton, closeButton);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        titleBar.getChildren().addAll(logoTitleGroup, menuBar, spacer, buttonContainer);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        newFile.setOnAction(e -> editorHandler.handleNewFile(stage));
        openFile.setOnAction(e -> editorHandler.handleOpenFile(stage));
        saveFile.setOnAction(e -> editorHandler.handleSaveFile(stage));
        saveAsFile.setOnAction(e -> editorHandler.handleSaveAsFile(stage));
        exit.setOnAction(e -> ScreenManager.switchToLauncher((Stage) root.getScene().getWindow()));

        return titleBar;
    }

    private Button createTitleBarButton(String symbol, Runnable action) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add("title-bar-button");
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private VBox leftNav() {
        VBox leftNav = new VBox();
        leftNav.getStyleClass().add("left-nav");

        VBox projectToolbar = createProjectToolbar();
        VBox projectDirectory = createProjectDirectory();

        Button toggleBtn = (Button) projectToolbar.getChildren().get(0);
        AtomicBoolean isVisible = new AtomicBoolean(true);

        toggleBtn.setOnAction(e -> {
            isVisible.set(!isVisible.get());
            projectDirectory.setVisible(isVisible.get());
            projectDirectory.setManaged(isVisible.get());

            FontIcon icon = (FontIcon) toggleBtn.getGraphic();
            icon.setIconCode(isVisible.get()
                    ? MaterialDesignF.FOLDER
                    : MaterialDesignF.FOLDER_OPEN);
        });

        StackPane directoryContainer = new StackPane(projectDirectory);
        HBox container = new HBox(projectToolbar, directoryContainer);

        leftNav.getChildren().addAll(container);
        return leftNav;
    }

    private VBox createProjectToolbar() {
        VBox toolbar = new VBox();
        toolbar.getStyleClass().add("project-toolbar");

        FontIcon folderIcon = new FontIcon("mdi2f-folder");
        Button toggleBtn = new Button("", folderIcon);
        toggleBtn.getStyleClass().add("folder-toggle-btn");

        toolbar.getChildren().addAll(toggleBtn);
        return toolbar;
    }

    private VBox createProjectDirectory() {
        VBox directory = new VBox();
        directory.getStyleClass().add("project-directory");

        Label directoryLabel = new Label("Project");
        directoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            Label error = new Label("Invalid or empty project path.");
            error.setStyle("-fx-text-fill: red;");
            directory.getChildren().addAll(directoryLabel, error);
            return directory;
        }

        TreeItem<Path> rootItem = createTreeItem(projectPath);
        rootItem.setExpanded(true);
        projectTree = new TreeView<>(rootItem);
        projectTree.setShowRoot(true);

        projectTree.setCellFactory(param -> new TreeCell<Path>() {
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

        directory.getChildren().addAll(directoryLabel, projectTree);
        return directory;
    }

    private TreeItem<Path> createTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        item.setGraphic(new Label(path.getFileName().toString()));

        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    item.getChildren().add(createTreeItem(child));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return item;
    }

    private void openFileInEditor(Path filePath) {
        if (editorTabPane == null) {
            System.err.println("EditorTabPane not initialized.");
            return;
        }

        // Avoid opening duplicate tabs
        for (Tab tab : editorTabPane.getTabs()) {
            if (tab.getText().equals(filePath.getFileName().toString())) {
                editorTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        String fileName = filePath.getFileName().toString();
        Tab fileTab = new Tab(fileName);
        fileTab.setClosable(true);

        CodeArea editorArea = new CodeArea();
        editorArea.setWrapText(true);
        editorArea.setParagraphGraphicFactory(LineNumberFactory.get(editorArea));
        editorArea.getStyleClass().add("editor-area");

        try {
            String content = Files.readString(filePath);
            editorArea.replaceText(content);
        } catch (IOException e) {
            editorArea.replaceText("Error reading file.");
        }

        ScrollPane scrollPane = new ScrollPane(editorArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        fileTab.setContent(scrollPane);
        editorTabPane.getTabs().add(fileTab);
        editorTabPane.getSelectionModel().select(fileTab);
    }


    private void setupStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");

        Label statusLabel = new Label("Ready");
        statusBar.getChildren().add(statusLabel);

        root.setBottom(statusBar);
    }

    private void setupEditorArea() {
        editorTabPane = new TabPane();
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        editorTabPane.setTabMinWidth(100);
        editorTabPane.setStyle(String.format("-fx-background-color: %s;", Color.BLACK));

        codeArea = new CodeArea();
        codeArea.setEditable(true);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setStyle("-fx-background-color: black; -fx-text-fill: white;");

        scrollPane = new ScrollPane(codeArea);
        scrollPane.setFitToWidth(true);

        root.setCenter(editorTabPane);
    }


}
