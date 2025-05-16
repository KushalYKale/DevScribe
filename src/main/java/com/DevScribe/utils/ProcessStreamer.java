package com.DevScribe.utils;

import javafx.application.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ProcessStreamer {

    // Stream output by reading small buffers of bytes, converting to String and passing immediately
    public static void streamOutput(InputStream inputStream, Consumer<String> outputConsumer) {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    // Ensure UI updates happen on the JavaFX Application thread
                    Platform.runLater(() -> outputConsumer.accept(chunk));
                }
            } catch (IOException e) {
                Platform.runLater(() -> outputConsumer.accept("[ERROR] Error reading stream: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    // Just reuse the same for error stream
    public static void streamError(InputStream errorStream, Consumer<String> errorConsumer) {
        streamOutput(errorStream, errorConsumer);
    }
}
