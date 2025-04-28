package com.DevScribe.ui.screen;

import com.DevScribe.ui.dialogs.NewProjectHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class LauncherScreen {
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isDarkMode = true; // Default to dark mode
    private BorderPane root;
    private ToggleButton themeToggle;
    private ListView<String> projectListView;
    private ObservableList<String> projectList;
    private Stage stage;  // Declare stage as an instance variable

    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        // Load the project list from the file
        projectList = FXCollections.observableArrayList(loadProjectList());

        root = new BorderPane();
        root.getStyleClass().add("root");

        root.setTop(createHeader(stage));
        root.setLeft(createLeftNav());
        root.setCenter(createContentArea());

        // Initialize the scene and pass the theme to it
        Scene scene = new Scene(root, 850, 725);
        scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());
        updateTheme(scene);

        // Theme toggle button
        themeToggle = new ToggleButton("Switch to Light");
        themeToggle.setOnAction(e -> toggleTheme());
        themeToggle.getStyleClass().add("toolbar-button"); // Reuse existing style
        themeToggle.setMaxWidth(Double.MAX_VALUE);

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(5, 15, 5, 15));
        bottomBar.getChildren().add(themeToggle);

        root.setBottom(bottomBar);

        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeToggle.setText(isDarkMode ? "Switch to Light" : "Switch to Dark");

        Scene scene = root.getScene();
        updateTheme(scene);
    }

    private void updateTheme(Scene scene) {
        scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
        scene.getRoot().getStyleClass().add(isDarkMode ? "dark-theme" : "light-theme");
    }

    private HBox createHeader(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(16);
        logoView.setPreserveRatio(true);

        Label title = new Label("DevScribe Launcher");
        title.getStyleClass().add("header-title");

        HBox logoTitleGroup = new HBox(3);
        logoTitleGroup.setAlignment(Pos.CENTER_LEFT);
        logoTitleGroup.getChildren().addAll(logoView, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximizeButton = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button closeButton = createTitleBarButton("\uE8BB", stage::close);
        closeButton.getStyleClass().add("close-button");

        titleBar.getChildren().addAll(logoTitleGroup, spacer, minimizeButton, maximizeButton, closeButton);

        // Draggable window
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
    }

    private Button createTitleBarButton(String symbol, Runnable action) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add("title-bar-button");
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private VBox createLeftNav() {
        VBox leftNav = new VBox(20);
        leftNav.setPadding(new Insets(20));
        leftNav.setPrefWidth(250);
        leftNav.getStyleClass().add("left-nav");

        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logo.setFitHeight(50);
        logo.setPreserveRatio(true);

        Label title = new Label("DevScribe");
        title.getStyleClass().add("nav-title");

        Label versionLabel = new Label("Version 1.0.0");
        versionLabel.getStyleClass().add("version-label");

        VBox titleVersion = new VBox(2);
        titleVersion.getChildren().addAll(title, versionLabel);
        titleVersion.setAlignment(Pos.CENTER_LEFT);

        HBox logoTitle = new HBox(10);
        logoTitle.setAlignment(Pos.CENTER_LEFT);
        logoTitle.getChildren().addAll(logo, titleVersion);

        leftNav.getChildren().add(logoTitle);
        return leftNav;
    }

    private BorderPane createContentArea() {
        BorderPane contentArea = new BorderPane();
        contentArea.setStyle("-fx-background-color: #2E2E38;");

        contentArea.setTop(createToolbar());

        // Initialize project list
        projectListView = new ListView<>(projectList);
        projectListView.getStyleClass().add("project-list");

        projectListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // Double-click to open project
                String selectedProject = projectListView.getSelectionModel().getSelectedItem();
                if (selectedProject != null) {
                    Path projectPath = Paths.get("path/to/projects", selectedProject);  // Adjust path accordingly
                    EditorScreen editorScreen = new EditorScreen();
                    editorScreen.start(stage, projectPath);  // Open in editor
                }
            }
        });

        VBox mainContent = createMainContent();

        // Add project list directly into the main content
        mainContent.getChildren().add(projectListView);

        // Add main content to scroll pane
        ScrollPane scrollContent = new ScrollPane();
        scrollContent.setFitToWidth(true);
        scrollContent.getStyleClass().add("scroll-content");
        scrollContent.setContent(mainContent);

        contentArea.setCenter(scrollContent);
        return contentArea;
    }

    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getStyleClass().add("main-content");
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(8, 15, 8, 15));
        toolbar.getStyleClass().add("toolbar");

        ImageView searchIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/search.png")));
        searchIcon.setFitHeight(16);
        searchIcon.setPreserveRatio(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Search projects...");
        searchField.getStyleClass().add("search-field");

        HBox searchBox = new HBox(8);
        searchBox.getStyleClass().add("search-box");
        searchBox.getChildren().addAll(searchIcon, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button newProjectBtn = createToolbarButton("New Project");
        Button openBtn = createToolbarButton("Open");
        Button cloneBtn = createToolbarButton("Clone Repository");

        newProjectBtn.setOnAction(e -> {
            NewProjectHandler.showNewProjectDialog(stage, new NewProjectHandler.ProjectCreationCallback() {
                @Override
                public void onProjectCreated(Path projectPath) {
                    // Add the new project to the list
                    addProjectToList(projectPath.getFileName().toString(), projectPath.toString());

                    // Open the project in the editor
                    EditorScreen editorScreen = new EditorScreen();
                    editorScreen.start(stage, projectPath);  // Pass Stage and Path
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        });

        toolbar.getChildren().addAll(
                searchBox,
                new Separator(Orientation.VERTICAL),
                newProjectBtn,
                openBtn,
                cloneBtn
        );
        return toolbar;
    }

    private void addProjectToList(String projectName, String projectPath) {
        // Add the new project to the ObservableList which updates the ListView automatically
        projectList.add(projectName);

        // Save the updated project list to the file
        saveProjectList(projectList);
    }

    private Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("toolbar-button");
        return button;
    }

    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    // Save project list to a file
    private void saveProjectList(List<String> projects) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("projects.txt"))) {
            for (String project : projects) {
                writer.write(project);
                writer.newLine();
            }
        } catch (IOException e) {
            showError("Error saving project list: " + e.getMessage());
        }
    }

    // Load project list from a file
    private List<String> loadProjectList() {
        List<String> projects = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("projects.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                projects.add(line);
            }
        } catch (IOException e) {
            showError("Error loading project list: " + e.getMessage());
        }
        return projects;
    }
}
