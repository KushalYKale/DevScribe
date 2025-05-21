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
    private ProcessStreamer processStreamer;
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

        // Add Ctrl+C handling for interrupt
        terminalArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCtrlCInterrupt);

        // Prevent mouse from moving caret before promptPosition
        terminalArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (newPos.intValue() < promptPosition) {
                Platform.runLater(() -> terminalArea.moveTo(promptPosition));
            }
        });
    }

    private void handleCtrlCInterrupt(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.C) {
            event.consume();
            if (currentProcess != null && currentProcess.isAlive()) {
                // Attempt to send SIGINT to process
                try {
                    sendInterruptToProcess();
                    appendText("\n^C\n", "-fx-fill: orange; -fx-font-weight: bold;");
                } catch (Exception e) {
                    appendText("[ERROR] Failed to send interrupt: " + e.getMessage() + "\n", "-fx-fill: red;");
                }
            }
            appendPrompt();
        }
    }

    // Platform-dependent way to send SIGINT (Ctrl+C) to process
    private void sendInterruptToProcess() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // On Windows: forcibly destroy process (no SIGINT equivalent)
            currentProcess.destroy();
        } else {
            // On Unix-like: send SIGINT signal
            long pid = currentProcess.pid();
            Runtime.getRuntime().exec(new String[]{"kill", "-2", Long.toString(pid)});
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        int caretPos = terminalArea.getCaretPosition();

        // Disallow editing before promptPosition
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
            // Prevent deleting prompt or moving left into prompt area
            case HOME -> {
                event.consume();
                terminalArea.moveTo(promptPosition);
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
        if (processStreamer != null) {
            processStreamer.stopStreaming();
            processStreamer = null;
        }
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
        String pythonCmd = getPythonCommand();
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

        // Use ProcessStreamer here as well for compile output
        ProcessStreamer compileStreamer = new ProcessStreamer(compileProcess.getInputStream(), line -> appendText(line + "\n", detectStyleFromLine(line)));
        compileStreamer.startStreaming();

        int compileExitCode;
        try {
            compileExitCode = compileProcess.waitFor();
        } catch (InterruptedException e) {
            appendText("Compilation interrupted.\n");
            cleanupProcess();
            return;
        }
        compileStreamer.stopStreaming();

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

        // Create ProcessStreamer that reads both stdout and stderr
        processStreamer = new ProcessStreamer(currentProcess.getInputStream(), currentProcess.getErrorStream(),
                line -> {
                    // Handle Python auto-install on stderr lines
                    if (handlePythonAutoInstall && line.contains("ModuleNotFoundError")) {
                        String missingModule = extractPythonMissingModule(line);
                        if (missingModule != null && !pythonRetry.get()) {
                            appendText("\n[INFO] Missing Python module detected: " + missingModule + "\n", "-fx-fill: orange; -fx-font-weight: bold;");
                            boolean installed = installPythonModule(missingModule);
                            if (installed) {
                                appendText("[INFO] Module '" + missingModule + "' installed. Retrying...\n", "-fx-fill: green;");
                                pythonRetry.set(true);
                                if (currentProcess != null) {
                                    currentProcess.destroy();
                                }
                                try {
                                    runPythonWithAutoInstall(Path.of(command[1]));
                                } catch (IOException e) {
                                    appendText("[ERROR] Failed to rerun Python script: " + e.getMessage() + "\n", "-fx-fill: red;");
                                }
                                return; // Skip printing this line again
                            }
                        }
                    }
                    appendText(line + "\n", detectStyleFromLine(line));
                });

        processStreamer.startStreaming();

        appendPrompt();

        new Thread(() -> {
            try {
                int exitCode = currentProcess.waitFor();
                processStreamer.stopStreaming();
                appendText("\nProcess exited with code: " + exitCode + "\n");
                cleanupProcess();
            } catch (InterruptedException e) {
                appendText("\nProcess was interrupted.\n");
                cleanupProcess();
            }
        }).start();
    }

    private boolean installPythonModule(String module) {
        try {
            appendText("[INFO] Installing Python module '" + module + "' via pip...\n");
            ProcessBuilder pb = new ProcessBuilder(getPythonCommand(), "-m", "pip", "install", module);
            Process p = pb.start();

            // Stream output synchronously (blocking)
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                 BufferedReader errBr = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) appendText(line + "\n");
                while ((line = errBr.readLine()) != null) appendText(line + "\n");
            }

            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            appendText("[ERROR] Failed to install Python module: " + e.getMessage() + "\n");
            return false;
        }
    }

    private String extractPythonMissingModule(String line) {
        Pattern p = Pattern.compile("No module named '([^']+)'");
        Matcher m = p.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String getPythonCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
    }

    private String getFileExtension(Path filePath) {
        String name = filePath.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? "" : name.substring(lastDot + 1).toLowerCase();
    }

    private void startInteractiveShell() throws IOException {
        String shellCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd.exe" : "/bin/bash";
        runProcess(shellCommand);
    }

    private String detectStyleFromLine(String line) {
        if (line.toLowerCase().contains("error")) {
            return "-fx-fill: red; -fx-font-weight: bold;";
        } else if (line.toLowerCase().contains("warning")) {
            return "-fx-fill: orange;";
        }
        return null;
    }
}
