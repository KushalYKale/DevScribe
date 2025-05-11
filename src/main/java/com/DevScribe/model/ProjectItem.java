package com.DevScribe.model;

import java.nio.file.Path;

public class ProjectItem {
    private final String name;
    private final Path path;

    public ProjectItem(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return name; // Display name in ListView
    }
}
