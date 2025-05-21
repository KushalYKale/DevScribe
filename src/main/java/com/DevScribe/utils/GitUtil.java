package com.DevScribe.utils;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.File;

public class GitUtil {

    public interface CloneProgressListener {
        void onProgress(String task, int completed, int total);
        void onCompleted();
        void onFailed(Exception e);
    }

    public static void cloneRepository(String uri, File directory, CloneProgressListener listener) {
        new Thread(() -> {
            try {
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(uri)
                        .setDirectory(directory)
                        .setProgressMonitor(new ProgressMonitor() {
                            private int totalTasks = 0;
                            private int completedTasks = 0;

                            @Override
                            public void start(int totalTasks) {
                                this.totalTasks = totalTasks;
                                this.completedTasks = 0;
                            }

                            @Override
                            public void beginTask(String title, int totalWork) {
                                listener.onProgress(title, 0, totalWork);
                            }

                            @Override
                            public void update(int completed) {
                                completedTasks += completed;
                                listener.onProgress("Cloning", completedTasks, totalTasks);
                            }

                            @Override
                            public void endTask() {
                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }

                            @Override
                            public void showDuration(boolean enabled) {
                                // Not needed, so leave empty
                            }
                        });

                try (Git git = cloneCommand.call()) {
                    listener.onCompleted();
                }
            } catch (Exception e) {
                listener.onFailed(e);
            }
        }).start();
    }
}

