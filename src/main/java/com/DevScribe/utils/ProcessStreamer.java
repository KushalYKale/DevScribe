package com.DevScribe.utils;

import javafx.scene.control.TextArea;

import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ProcessStreamer {
    public static void streamOutput(InputStream inputStream, TextArea terminal) {
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Append each line to the terminal's TextArea
                    terminal.appendText(line + "\n");
                }
            } catch (IOException e) {
                terminal.appendText("Error reading process output: " + e.getMessage() + "\n");
            }
        });
        outputThread.setDaemon(true);  // Allow thread to be terminated when the app closes
        outputThread.start();
    }
}
