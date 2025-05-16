package com.DevScribe.ui.dialogs;

import com.DevScribe.utils.ProcessStreamer;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Terminal extends VBox {
    private final InlineCssTextArea terminalArea;
    private OutputStream processInput;
    private Process currentProcess;

    private int promptPosition; // position where user input starts

    public Terminal() {
        terminalArea = new InlineCssTextArea();
        terminalArea.setEditable(false);
        terminalArea.setWrapText(true);
        terminalArea.getStyleClass().add("inline-css");
        this.getStylesheets().add(getClass().getResource("/css/terminal.css").toExternalForm());

        this.getChildren().add(terminalArea);
        VBox.setVgrow(terminalArea, Priority.ALWAYS);
        terminalArea.setPrefHeight(Region.USE_COMPUTED_SIZE);

        this.setPrefWidth(1000); // You can still set width manually
        this.setVisible(false);

        // Automatically resize height to 40% of window
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                this.prefHeightProperty().bind(newScene.heightProperty().multiply(0.4));
            }
        });

        terminalArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }


    private void handleKeyPressed(KeyEvent event) {
        int caretPos = terminalArea.getCaretPosition();

        // Block editing before promptPosition
        if (caretPos < promptPosition) {
            terminalArea.moveTo(terminalArea.getLength());
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.ENTER) {
            event.consume();

            String input = terminalArea.getText(promptPosition, terminalArea.getLength());
            input = input.replace("\n", ""); // remove newline

            appendText("\n"); // move to new line

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

            // Update prompt position so user can continue typing
            promptPosition = terminalArea.getLength();

        } else if (event.getCode() == KeyCode.BACK_SPACE) {
            if (caretPos <= promptPosition) {
                event.consume();
            }
        } else if (event.getCode() == KeyCode.LEFT) {
            if (caretPos <= promptPosition) {
                event.consume();
            }
        }
    }

    public void appendText(String text) {
        Platform.runLater(() -> {
            try {
                terminalArea.appendText(text);
                promptPosition = terminalArea.getLength();
                terminalArea.moveTo(promptPosition);
            } catch (Exception ex) {
                System.out.println("[FX ERROR] " + ex.getMessage());
            }
        });
    }

    public void appendPrompt() {
        Platform.runLater(() -> {
            if (terminalArea.getLength() > 0) {
                char lastChar = terminalArea.getText(terminalArea.getLength() - 1, terminalArea.getLength()).charAt(0);
                if (lastChar != '\n') {
                    terminalArea.appendText("\n");
                }
            }
            terminalArea.appendText("> ");
            promptPosition = terminalArea.getLength();
            terminalArea.moveTo(promptPosition);
            terminalArea.setEditable(true);  // Enable typing at prompt
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
                    // No file, start interactive shell mode
                    startInteractiveShell();
                    return;
                }

                switch (fileExtension) {
                    case "java" -> runJavaFile(filePath, code);
                    case "py" -> runProcess("python", filePath.toString());
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

        // Compile
        ProcessBuilder compileBuilder = new ProcessBuilder("javac", "-cp", classpath, fileName);
        compileBuilder.directory(parentDir.toFile());
        compileBuilder.redirectErrorStream(true);
        Process compileProcess = compileBuilder.start();

        ProcessStreamer.streamOutput(compileProcess.getInputStream(), this::appendText);
        int compileExitCode = compileProcess.waitFor();

        if (compileExitCode != 0) {
            appendText("Compilation failed.\n");
            cleanupProcess();
            return;
        }

        // Run with working directory
        runProcessWithDir(new String[]{"java", "-cp", classpath, fullClassName}, parentDir.toFile());
    }

    private void runProcess(String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        currentProcess = builder.start();

        setProcessInput(currentProcess.getOutputStream());

        InputStream stdout = currentProcess.getInputStream();
        ProcessStreamer.streamOutput(stdout, this::appendText);

        // Enable typing right after process starts
        appendPrompt();

        // Wait for process exit asynchronously
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

    private void runProcessWithDir(String[] command, java.io.File workingDir) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
        currentProcess = builder.start();

        setProcessInput(currentProcess.getOutputStream());

        InputStream stdout = currentProcess.getInputStream();
        ProcessStreamer.streamOutput(stdout, this::appendText);

        // Enable typing right after process starts
        appendPrompt();

        // Wait for process exit asynchronously
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

    private void startInteractiveShell() throws IOException {
        String shellCmd;
        String shellFlag;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            shellCmd = "cmd";
            shellFlag = "/K"; // keep shell open
        } else {
            shellCmd = "bash";
            shellFlag = "-i"; // interactive shell
        }

        ProcessBuilder shellBuilder = new ProcessBuilder(shellCmd, shellFlag);
        shellBuilder.redirectErrorStream(true);
        currentProcess = shellBuilder.start();

        setProcessInput(currentProcess.getOutputStream());

        InputStream stdout = currentProcess.getInputStream();
        ProcessStreamer.streamOutput(stdout, this::appendText);

        // Enable typing immediately
        appendPrompt();

        // Wait for shell exit asynchronously
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
    }

    private String getFileExtension(Path filePath) {
        if (filePath == null) return "";
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
