package com.DevScribe.ui.screen;

import com.DevScribe.model.ProjectItem;
import com.DevScribe.ui.dialogs.NewProjectHandler;
import com.DevScribe.ui.dialogs.ProgressDialog;
import com.DevScribe.utils.GitUtil;
import com.DevScribe.utils.PathValidator;
import com.DevScribe.utils.ScreenManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class LauncherScreen {
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isDarkMode = true;
    private BorderPane root;
    private ToggleButton themeToggle;
    private ListView<ProjectItem> projectListView;
    private ObservableList<ProjectItem> projectList;
    private FilteredList<ProjectItem> filteredList;
    private Stage stage;

    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);

        projectList = FXCollections.observableArrayList(loadProjectList());
        filteredList = new FilteredList<>(projectList, p -> true);

        root = new BorderPane();
        root.getStyleClass().add("root");

        root.setTop(createHeader(stage));
        root.setLeft(createLeftNav());
        root.setCenter(createContentArea());

        Scene scene = new Scene(root, 850, 725);
        scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());
        updateTheme(scene);

        themeToggle = new ToggleButton("Switch to Light");
        themeToggle.setOnAction(e -> toggleTheme());
        themeToggle.getStyleClass().add("toolbar");

        HBox bottomBar = new HBox(themeToggle);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(5, 15, 5, 15));
        root.setBottom(bottomBar);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void toggleTheme() {
        isDarkMode = themeToggle.isSelected();
        ScreenManager.setDarkMode(isDarkMode);
        themeToggle.setText(isDarkMode ? "Switch to Light" : "Switch to Dark");
        updateTheme(root.getScene());
    }

    private void updateTheme(Scene scene) {
        scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
        scene.getRoot().getStyleClass().add(isDarkMode ? "dark-theme" : "light-theme");

        projectListView.getStyleClass().removeAll("project-list-dark", "project-list-light");
        projectListView.getStyleClass().add(isDarkMode ? "project-list-dark" : "project-list-light");
    }

    private HBox createHeader(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(16);
        logoView.setPreserveRatio(true);

        Label title = new Label("DevScribe Launcher");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimize = createTitleBarButton("\uE921", () -> stage.setIconified(true));
        Button maximize = createTitleBarButton("\uE923", () -> stage.setMaximized(!stage.isMaximized()));
        Button close = createTitleBarButton("\uE8BB", stage::close);
        close.getStyleClass().add("close-button");

        titleBar.getChildren().addAll(new HBox(3, logoView, title), spacer, minimize, maximize, close);

        titleBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        titleBar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        return titleBar;
    }

    private Button createTitleBarButton(String symbol, Runnable action) {
        Button btn = new Button(symbol);
        btn.setFont(Font.font("Segoe MDL2 Assets", FontWeight.BOLD, 13));
        btn.getStyleClass().add("title-bar-button");
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

        VBox titleVersion = new VBox(2, title, versionLabel);
        HBox logoTitle = new HBox(10, logo, titleVersion);
        logoTitle.setAlignment(Pos.CENTER_LEFT);

        leftNav.getChildren().add(logoTitle);
        return leftNav;
    }

    private BorderPane createContentArea() {
        BorderPane contentArea = new BorderPane();
        contentArea.setTop(createToolbar());

        projectListView = new ListView<>(filteredList);
        projectListView.getStyleClass().add(isDarkMode ? "project-list-dark" : "project-list-light");
        projectListView.getStyleClass().add("project-list");

        projectListView.setCellFactory(listView -> {
            ListCell<ProjectItem> cell = new ListCell<>() {
                @Override
                protected void updateItem(ProjectItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        VBox infoBox = new VBox(5);
                        infoBox.setPadding(new Insets(8));
                        infoBox.getStyleClass().add("project-list");

                        Label nameLabel = new Label(item.getName());
                        nameLabel.getStyleClass().add("project-list-label");

                        Label pathLabel = new Label(item.getPath().toString());
                        pathLabel.getStyleClass().add("project-path");

                        Button deleteBtn = new Button("❌");
                        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff6666; -fx-font-size: 14px;");
                        deleteBtn.setOnAction(e -> {
                            projectList.remove(item);
                            saveProjectList(projectList);
                        });

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        HBox bottomRow = new HBox(pathLabel, spacer, deleteBtn);
                        bottomRow.setAlignment(Pos.CENTER_LEFT);
                        bottomRow.setSpacing(10);

                        infoBox.getChildren().addAll(nameLabel, bottomRow);
                        setGraphic(infoBox);
                    }
                }

            };

            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    ProjectItem item = cell.getItem();
                    ScreenManager.switchToEditor((Stage) listView.getScene().getWindow(), item.getPath());
                }
            });
            return cell;
        });

        VBox mainContent = createMainContent();
        mainContent.getChildren().add(projectListView);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-content");

        contentArea.setCenter(scrollPane);
        return contentArea;
    }

    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getStyleClass().add("com.DevScribe.main-content");
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(8, 15, 8, 15));
        toolbar.getStyleClass().add("toolbar");

        TextField searchField = new TextField();
        searchField.setPromptText("Search projects...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(item -> item.getName().toLowerCase().contains(newVal.toLowerCase()));
        });

        ImageView searchIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/search.png")));
        searchIcon.setFitHeight(16);
        searchIcon.setPreserveRatio(true);

        HBox searchBox = new HBox(8, searchIcon, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button newProjectBtn = createToolbarButton("New Project");
        newProjectBtn.setOnAction(e -> {
            NewProjectHandler.showNewProjectDialog(stage, new NewProjectHandler.ProjectCreationCallback() {
                @Override
                public void onProjectCreated(Path projectPath) {
                    addProjectToList(projectPath.getFileName().toString(), projectPath.toString());
                    if (Files.exists(projectPath) && Files.isDirectory(projectPath)) {
                        new EditorScreen().start(stage, projectPath, isDarkMode);
                    } else {
                        showError("Invalid project path.");
                    }
                }

                @Override
                public void onError(String message) {
                    showError(message);
                }
            });
        });

        Button openBtn = createToolbarButton("Open");
        openBtn.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Open Project Directory");
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                Path projectPath = selectedDirectory.toPath().toAbsolutePath();
                if (!PathValidator.validateProjectPath(projectPath)) {
                    showError("Selected folder is not a valid project.");
                    return;
                }
                addProjectToList(selectedDirectory.getName(), projectPath.toString());
                new EditorScreen().start(stage, projectPath, isDarkMode);
            }
        });

        Button cloneBtn = createToolbarButton("Clone Repository");
        cloneBtn.setOnAction(e -> {
            TextInputDialog urlDialog = new TextInputDialog();
            urlDialog.setTitle("Clone Git Repository");
            urlDialog.setHeaderText("Enter the Git Repository URL to clone:");
            urlDialog.setContentText("Repository URL:");

            urlDialog.showAndWait().ifPresent(url -> {
                if (url.isEmpty()) {
                    showError("Repository URL cannot be empty.");
                    return;
                }

                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Select Folder to Clone Repository Into");
                File selectedDir = dirChooser.showDialog(stage);
                if (selectedDir == null) {
                    showError("No folder selected for cloning.");
                    return;
                }

                String folderName = extractRepoNameFromUrl(url);
                File cloneDir = new File(selectedDir, folderName);

                ProgressDialog progressDialog = new ProgressDialog(stage, "Cloning Repository...");
                progressDialog.show();

                GitUtil.cloneRepository(url, cloneDir, new GitUtil.CloneProgressListener() {
                    @Override
                    public void onProgress(String task, int completed, int total) {
                        double prog = total > 0 ? (double) completed / total : -1;
                        javafx.application.Platform.runLater(() -> {
                            progressDialog.setMessage(task);
                            progressDialog.setProgress(prog);
                        });
                    }

                    @Override
                    public void onCompleted() {
                        javafx.application.Platform.runLater(() -> {
                            progressDialog.close();
                            addProjectToList(folderName, cloneDir.getAbsolutePath());
                            new EditorScreen().start(stage, cloneDir.toPath(), isDarkMode);
                        });
                    }

                    @Override
                    public void onFailed(Exception e) {
                        javafx.application.Platform.runLater(() -> {
                            progressDialog.close();
                            showError("Failed to clone repository:\n" + e.getMessage());
                        });
                    }
                });
            });
        });

        toolbar.getChildren().addAll(searchBox, new Separator(Orientation.VERTICAL), newProjectBtn, openBtn, cloneBtn);
        return toolbar;
    }

    private String extractRepoNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "repository";
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        int lastSlash = url.lastIndexOf('/');
        String repoName = url.substring(lastSlash + 1);
        return repoName.endsWith(".git") ? repoName.substring(0, repoName.length() - 4) : repoName;
    }

    private Button createToolbarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("toolbar-button");
        return button;
    }

    private void addProjectToList(String name, String path) {
        ProjectItem item = new ProjectItem(name, Paths.get(path));
        projectList.add(item);
        saveProjectList(projectList);
    }

    private List<ProjectItem> loadProjectList() {
        List<ProjectItem> items = new ArrayList<>();
        File file = new File("projects.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path path = Paths.get(line);
                    items.add(new ProjectItem(path.getFileName().toString(), path));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    private void saveProjectList(List<ProjectItem> list) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("projects.txt"))) {
            for (ProjectItem item : list) {
                writer.write(item.getPath().toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
