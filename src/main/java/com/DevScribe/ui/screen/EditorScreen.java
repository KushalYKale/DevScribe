package com.DevScribe.ui.screen;

import com.DevScribe.ui.components.EditorHandler;
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

import java.util.concurrent.atomic.AtomicBoolean;

public class EditorScreen {
    private double xOffset = 0;
    private double yOffset = 0;
    private BorderPane root;
    private EditorHandler editorHandler = new EditorHandler();
    private TabPane editorTabPane;
    private ScrollPane scrollPane;
    private CodeArea codeArea;


    public void start(Stage stage) {
        root = new BorderPane();  // Initialize the class field

        root.setTop(createHeader(stage));
        root.setLeft(leftNav());
        setupEditorArea();
        setupStatusBar();

        Scene scene = new Scene(root, 1400, 750);
        scene.getStylesheets().add(getClass().getResource("/css/editor.css").toExternalForm());
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private HBox createHeader(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(16);
        logoView.setPreserveRatio(true);

        Label title = new Label("DevScribe");
        title.getStyleClass().add("header-title");

        HBox logoTitleGroup = new HBox(3);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);
        logoTitleGroup.getChildren().addAll(logoView, title);

        // Create menu bar
        HBox menuBar = new HBox(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        // File menu
        MenuButton fileMenu = new MenuButton("File");
        MenuItem newFile = new MenuItem("New");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem saveAsFile = new MenuItem("Save As");
        MenuItem exit = new MenuItem("Exit");
        fileMenu.getItems().addAll(newFile, openFile, saveFile, saveAsFile, new SeparatorMenuItem(), exit);

        // Edit menu
        MenuButton editMenu = new MenuButton("Edit");
        MenuItem undo = new MenuItem("Undo");
        MenuItem redo = new MenuItem("Redo");
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        editMenu.getItems().addAll(undo, redo, new SeparatorMenuItem(), cut, copy, paste);

        // View menu
        MenuButton viewMenu = new MenuButton("View");
        CheckMenuItem wordWrap = new CheckMenuItem("Word Wrap");
        MenuItem zoomIn = new MenuItem("Zoom In");
        MenuItem zoomOut = new MenuItem("Zoom Out");
        viewMenu.getItems().addAll(wordWrap, new SeparatorMenuItem(), zoomIn, zoomOut);

        // Add menus to menu bar
        menuBar.getChildren().addAll(fileMenu, editMenu, viewMenu);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button runButton = createTitleBarButton("\u25B6", () -> {
            // Add functionality to run the code
            System.out.println("Run button clicked");
        });
        runButton.getStyleClass().add("run-button");

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        HBox buttonContainer = new HBox(8);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.getChildren().addAll(runButton, minimizeButton, maximizeButton, closeButton);

        titleBar.getChildren().addAll(logoTitleGroup, menuBar, spacer, buttonContainer);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // Add action handlers for menu items
        newFile.setOnAction(e -> editorHandler.handleNewFile(stage));
        openFile.setOnAction(e -> editorHandler.handleOpenFile());
        saveFile.setOnAction(e -> editorHandler.handleSaveFile());
        saveAsFile.setOnAction(e -> editorHandler.handleSaveAsFile());
        exit.setOnAction(e -> {
            Stage newStage = new Stage();
            LauncherScreen launcherScreen = new LauncherScreen();
            try {
                launcherScreen.start(newStage);
                stage.close();  // Close the current stage
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        cut.setOnAction(e -> editorHandler.handleCut());
        redo.setOnAction(e -> editorHandler.handleRedo());
        copy.setOnAction(e -> editorHandler.handleCopy());
        undo.setOnAction(e -> editorHandler.handleUndo());
        paste.setOnAction(e -> editorHandler.handlePaste());

        zoomIn.setOnAction(e -> editorHandler.handleZoomIn());
        zoomOut.setOnAction(e -> editorHandler.handleZoomOut());
        wordWrap.setOnAction(e -> editorHandler.handleWordWrap(wordWrap.isSelected()));

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

        // Toggle functionality
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

        // Use StackPane to maintain layout when hidden
        StackPane directoryContainer = new StackPane(projectDirectory);
        HBox container = new HBox(projectToolbar, directoryContainer);

        leftNav.getChildren().add(container);
        return leftNav;
    }

    private VBox createProjectToolbar() {
        VBox toolbar = new VBox();
        toolbar.getStyleClass().add("project-toolbar");

        // Folder toggle button
        FontIcon folderIcon = new FontIcon("mdi2f-folder");
        Button toggleBtn = new Button("", folderIcon);
        toggleBtn.getStyleClass().add("folder-toggle-btn");

        // Add other toolbar buttons if needed
        // Button refreshBtn = new Button("", new FontIcon("mdi2f-refresh"));

        toolbar.getChildren().addAll(toggleBtn /*, refreshBtn */);
        return toolbar;
    }

    private VBox createProjectDirectory() {
        VBox directory = new VBox();
        directory.getStyleClass().add("project-directory");

        Label directoryLabel = new Label("Project Directory");
        directoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        directory.getChildren().add(directoryLabel);

        return directory;
    }


    private void setupEditorArea() {
        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add("code-tab-pane");
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Create reusable CodeArea
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        // Wrap CodeArea in ScrollPane
        scrollPane = new ScrollPane(codeArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Bind CodeArea size to ScrollPane for dynamic resizing
        codeArea.prefWidthProperty().bind(scrollPane.widthProperty());
        codeArea.prefHeightProperty().bind(scrollPane.heightProperty());

        // Create initial tab
        Tab initialTab = new Tab("Untitled");
        initialTab.getStyleClass().add("code-tab");
        initialTab.setContent(scrollPane);
        editorTabPane.getTabs().add(initialTab);

        // Set the editor to center of root layout
        root.setCenter(editorTabPane);
    }


    private void setupStatusBar() {
        Label statusBar = new Label("Ready");
        statusBar.getStyleClass().add("status-bar");
        root.setBottom(statusBar);
    }
}