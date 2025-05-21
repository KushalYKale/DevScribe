package com.DevScribe.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ProcessStreamer {

    private final InputStream stdout;
    private final InputStream stderr;
    private final Consumer<String> textConsumer;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean running = false;

    public ProcessStreamer(InputStream stdout, InputStream stderr, Consumer<String> textConsumer) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.textConsumer = textConsumer;
    }

    public ProcessStreamer(InputStream stdout, Consumer<String> textConsumer) {
        this(stdout, null, textConsumer);
    }

    public void startStreaming() {
        running = true;
        executor.submit(() -> streamOutput(stdout));
        if (stderr != null) {
            executor.submit(() -> streamOutput(stderr));
        }
    }

    private void streamOutput(InputStream inputStream) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            while (running && (bytesRead = inputStream.read(buffer)) != -1) {
                String text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                textConsumer.accept(text);
            }
        } catch (IOException e) {
            if (running) {
                textConsumer.accept("[ProcessStreamer ERROR] " + e.getMessage());
            }
        }
    }

    public void stopStreaming() {
        running = false;
        executor.shutdownNow();
    }
}
