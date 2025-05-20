package com.DevScribe.ui.dialogs;

import com.DevScribe.utils.ProcessStreamer;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terminal extends VBox {
    private final InlineCssTextArea terminalArea;
    private OutputStream processInput;
    private Process currentProcess;
    private int promptPosition;

    // To track if a python retry after install is done
    private AtomicBoolean pythonRetry = new AtomicBoolean(false);

    public Terminal() {
        terminalArea = new InlineCssTextArea();
        terminalArea.setEditable(false);
        terminalArea.setWrapText(true);
        terminalArea.getStyleClass().add("inline-css");
        this.getStylesheets().add(getClass().getResource("/css/terminal.css").toExternalForm());

        this.getChildren().add(terminalArea);
        VBox.setVgrow(terminalArea, Priority.ALWAYS);
        terminalArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        this.setPrefWidth(1000);
        this.setVisible(false);

        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                this.prefHeightProperty().bind(newScene.heightProperty().multiply(0.4));
            }
        });

        terminalArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void handleKeyPressed(KeyEvent event) {
        int caretPos = terminalArea.getCaretPosition();
        if (caretPos < promptPosition) {
            terminalArea.moveTo(terminalArea.getLength());
            event.consume();
            return;
        }

        switch (event.getCode()) {
            case ENTER -> {
                event.consume();
                String input = terminalArea.getText(promptPosition, terminalArea.getLength()).replace("\n", "");
                appendText("\n");
                if (processInput != null) {
                    try {
                        processInput.write((input + "\n").getBytes(StandardCharsets.UTF_8));
                        processInput.flush();
                    } catch (IOException ex) {
                        appendText("[ERROR] Failed to write input: " + ex.getMessage() + "\n");
                    }
                } else {
                    appendText("[ERROR] No process input stream.\n");
                }
                promptPosition = terminalArea.getLength();
            }
            case BACK_SPACE, LEFT -> {
                if (caretPos <= promptPosition) {
                    event.consume();
                }
            }
        }
    }

    // Append text with optional style
    public void appendText(String text) {
        appendText(text, null);
    }
    public void appendText(String text, String style) {
        Platform.runLater(() -> {
            if (style == null || style.isEmpty()) {
                terminalArea.appendText(text);
            } else {
                int start = terminalArea.getLength();
                terminalArea.appendText(text);
                int end = terminalArea.getLength();
                terminalArea.setStyle(start, end, style);
            }
            promptPosition = terminalArea.getLength();
            terminalArea.moveTo(promptPosition);
        });
    }

    public void appendPrompt() {
        Platform.runLater(() -> {
            if (terminalArea.getLength() > 0 &&
                    terminalArea.getText(terminalArea.getLength() - 1, terminalArea.getLength()).charAt(0) != '\n') {
                terminalArea.appendText("\n");
            }
            terminalArea.appendText("> ");
            promptPosition = terminalArea.getLength();
            terminalArea.moveTo(promptPosition);
            terminalArea.setEditable(true);
            terminalArea.requestFocus();
        });
    }

    public void clear() {
        Platform.runLater(() -> {
            terminalArea.clear();
            promptPosition = 0;
            terminalArea.setEditable(false);
        });
    }

    public void setProcessInput(OutputStream processInput) {
        this.processInput = processInput;
    }

    private void cleanupProcess() {
        if (currentProcess != null) {
            currentProcess.destroy();
            currentProcess = null;
        }
        processInput = null;
        Platform.runLater(() -> terminalArea.setEditable(false));
        pythonRetry.set(false);
    }

    public void showTerminal(Path filePath, String code) {
        Platform.runLater(() -> {
            this.setVisible(true);
            terminalArea.requestFocus();
            clear();
        });

        new Thread(() -> {
            try {
                String fileExtension = getFileExtension(filePath);

                if (fileExtension.isEmpty()) {
                    startInteractiveShell();
                    return;
                }

                switch (fileExtension) {
                    case "java" -> runJavaFile(filePath, code);
                    case "py" -> runPythonWithAutoInstall(filePath);
                    case "js" -> runProcess("node", filePath.toString());
                    case "sh" -> runProcess("bash", filePath.toString());
                    default -> appendText("Unsupported file type: " + fileExtension + "\n");
                }
            } catch (Exception e) {
                appendText("Error running the process: " + e.getMessage() + "\n");
                cleanupProcess();
            }
        }).start();
    }

    // Python run with auto-install
    private void runPythonWithAutoInstall(Path filePath) throws IOException {
        String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
        runProcessWithOutputHandling(new String[]{pythonCmd, filePath.toString()}, null, true);
    }

    private void runJavaFile(Path filePath, String code) throws IOException, InterruptedException {
        Path parentDir = filePath.getParent();
        String fileName = filePath.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        String sourceCode = code;

        String fullClassName = className;
        if (sourceCode.contains("package ")) {
            int start = sourceCode.indexOf("package ") + 8;
            int end = sourceCode.indexOf(";", start);
            String packageName = sourceCode.substring(start, end).trim();
            fullClassName = packageName + "." + className;
        }

        Path libDir = parentDir.resolve("lib");
        String classpath = libDir.toFile().exists()
                ? "lib/*" + (System.getProperty("os.name").toLowerCase().contains("win") ? ";." : ":.")
                : ".";

        ProcessBuilder compileBuilder = new ProcessBuilder("javac", "-cp", classpath, fileName);
        compileBuilder.directory(parentDir.toFile());
        compileBuilder.redirectErrorStream(true);
        appendText("Compiling Java file: " + fileName + "\n");

        Process compileProcess = compileBuilder.start();
        streamProcessOutputWithStyle(compileProcess.getInputStream());
        int compileExitCode;
        try {
            compileExitCode = compileProcess.waitFor();
        } catch (InterruptedException e) {
            appendText("Compilation interrupted.\n");
            cleanupProcess();
            return;
        }

        if (compileExitCode != 0) {
            appendText("Compilation failed. Check for missing packages or syntax errors.\n");
            cleanupProcess();
            return;
        }

        appendText("Running Java class: " + fullClassName + "\n");
        runProcessWithDir(new String[]{"java", "-cp", classpath, fullClassName}, parentDir.toFile());
    }

    // Basic runProcess wrapper
    private void runProcess(String... command) throws IOException {
        runProcessWithOutputHandling(command, null, false);
    }

    private void runProcessWithDir(String[] command, File workingDir) throws IOException {
        runProcessWithOutputHandling(command, workingDir, false);
    }

    // Core process runner with output parsing, styling, and python auto-install logic
    private void runProcessWithOutputHandling(String[] command, File workingDir, boolean handlePythonAutoInstall) throws IOException {
        appendText("Running command: " + String.join(" ", command) + "\n");

        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) builder.directory(workingDir);
        builder.redirectErrorStream(false); // handle stdout & stderr separately

        currentProcess = builder.start();

        setProcessInput(currentProcess.getOutputStream());

        InputStream stdout = currentProcess.getInputStream();
        InputStream stderr = currentProcess.getErrorStream();

        // Stream stdout
        new Thread(() -> streamProcessOutputWithStyle(stdout)).start();

        // Stream stderr for error parsing & Python auto-install
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (handlePythonAutoInstall && line.contains("ModuleNotFoundError")) {
                        String missingModule = extractPythonMissingModule(line);
                        if (missingModule != null && !pythonRetry.get()) {
                            appendText("\n[INFO] Missing Python module detected: " + missingModule + "\n", "-fx-fill: orange; -fx-font-weight: bold;");
                            boolean installed = installPythonModule(missingModule);
                            if (installed) {
                                appendText("[INFO] Module '" + missingModule + "' installed. Retrying...\n", "-fx-fill: green;");
                                pythonRetry.set(true);
                                currentProcess.destroy(); // Kill current process before retry
                                runPythonWithAutoInstall(Path.of(command[1]));
                                return; // Exit current thread
                            } else {
                                appendText("[ERROR] Failed to install module '" + missingModule + "'. Please install it manually.\n", "-fx-fill: red;");
                            }
                        }
                    }
                    appendText(line + "\n", detectStyleFromLine(line));
                }
            } catch (IOException e) {
                appendText("[ERROR] Error reading process stderr: " + e.getMessage() + "\n", "-fx-fill: red;");
            }
        }).start();

        appendPrompt();
        new Thread(() -> {
            try {
                int exitCode = currentProcess.waitFor();
                appendText("\nProcess exited with code: " + exitCode + "\n");
                cleanupProcess();
            } catch (InterruptedException e) {
                appendText("\nProcess interrupted.\n");
                cleanupProcess();
            }
        }).start();
    }

    // Extract missing Python module name from error line
    private String extractPythonMissingModule(String line) {
        Pattern p = Pattern.compile("No module named ['\"]([^'\"]+)['\"]");
        Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    // Run 'pip install <module>'
    private boolean installPythonModule(String module) {
        try {
            appendText("[INFO] Installing Python module '" + module + "'...\n", "-fx-fill: orange;");
            ProcessBuilder pb = new ProcessBuilder(getPythonCommand(), "-m", "pip", "install", module);
            Process p = pb.start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    appendText("[pip] " + line + "\n");
                }
            }

            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            appendText("[ERROR] Exception during pip install: " + e.getMessage() + "\n", "-fx-fill: red;");
            return false;
        }
    }

    private String getPythonCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
    }

    // Detect error/warning lines and assign styles
    private String detectStyleFromLine(String line) {
        String lower = line.toLowerCase();

        if (lower.contains("error:") || lower.contains("exception") || lower.startsWith("traceback")) {
            return "-fx-fill: red; -fx-font-weight: bold;";
        }

        if (lower.contains("warning:")) {
            return "-fx-fill: orange; -fx-font-weight: bold;";
        }

        if (lower.contains("gcc") && (lower.contains("error") || lower.contains("warning"))) {
            return lower.contains("error") ? "-fx-fill: red; -fx-font-weight: bold;" : "-fx-fill: orange; -fx-font-weight: bold;";
        }

        return null; // default no style
    }

    // Stream input stream lines into terminal with basic style detection
    private void streamProcessOutputWithStyle(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendText(line + "\n", detectStyleFromLine(line));
            }
        } catch (IOException e) {
            appendText("[ERROR] Reading process output failed: " + e.getMessage() + "\n", "-fx-fill: red;");
        }
    }

    private void startInteractiveShell() {
        try {
            String shell = System.getenv().getOrDefault("SHELL", System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "/bin/bash");
            ProcessBuilder builder = new ProcessBuilder(shell);
            currentProcess = builder.start();
            setProcessInput(currentProcess.getOutputStream());

            // Stream stdout
            new Thread(() -> streamProcessOutputWithStyle(currentProcess.getInputStream())).start();
            // Stream stderr
            new Thread(() -> streamProcessOutputWithStyle(currentProcess.getErrorStream())).start();

            appendPrompt();

            new Thread(() -> {
                try {
                    int exitCode = currentProcess.waitFor();
                    appendText("\nShell exited with code: " + exitCode + "\n");
                    cleanupProcess();
                } catch (InterruptedException e) {
                    appendText("\nShell interrupted.\n");
                    cleanupProcess();
                }
            }).start();

        } catch (IOException e) {
            appendText("[ERROR] Could not start interactive shell: " + e.getMessage() + "\n", "-fx-fill: red;");
            cleanupProcess();
        }
    }

    private static String getFileExtension(Path path) {
        String name = path.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? "" : name.substring(lastDot + 1).toLowerCase();
    }
}
