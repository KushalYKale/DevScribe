package com.DevScribe.ui.dialogs;

import com.DevScribe.utils.ProcessStreamer;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

import java.nio.file.Path;

public class Terminal extends VBox {
    private final TextArea terminal;

    public Terminal() {
        terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setWrapText(true);
        terminal.setStyle("""
             -fx-control-inner-background: #1e1e1e;
             -fx-text-fill: #00FF00;
             -fx-font-family: monospace;
             -fx-highlight-fill: #555555;
             -fx-highlight-text-fill: white;
        """);
        this.getChildren().add(terminal);
        this.setPrefHeight(200);
        this.setPrefWidth(1000);
        this.setVisible(false);
    }

    public TextArea getTerminal() {
        return terminal;
    }

    public void appendText(String text) {
        terminal.appendText(text);
    }

    public void clear() {
        terminal.clear();
    }

    // Display the terminal inside the existing window (no new stage)
    public void showTerminal(Path filePath, String code) {
        this.setVisible(true);  // Make the terminal visible in the main window

        try {
            String fileExtension = getFileExtension(filePath);

            ProcessBuilder processBuilder;

            if (fileExtension.equals("java")) {
                processBuilder = new ProcessBuilder("java", filePath.toString());
            } else if (fileExtension.equals("py")) {
                processBuilder = new ProcessBuilder("python", filePath.toString());
            } else if (fileExtension.equals("js")) {
                processBuilder = new ProcessBuilder("node", filePath.toString());
            } else if (fileExtension.equals("sh")) {
                processBuilder = new ProcessBuilder("bash", filePath.toString());
            } else {
                appendText("Unsupported file type: " + fileExtension + "\n");
                return;
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            ProcessStreamer.streamOutput(process.getInputStream(), terminal);
            process.waitFor();  // Wait until the process finishes
        } catch (Exception e) {
            appendText("Error running the process: " + e.getMessage() + "\n");
        }
    }

    // Utility method to get the file extension
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    public VBox getRoot() {
        return this;
    }
}
