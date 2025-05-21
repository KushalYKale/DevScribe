package com.DevScribe.ui.dialogs;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog {
    private Stage dialogStage;
    private ProgressBar progressBar;
    private Label messageLabel;

    public ProgressDialog(Stage owner, String title) {
        dialogStage = new Stage(StageStyle.UTILITY);
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);

        messageLabel = new Label("Starting...");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);

        VBox root = new VBox(10, messageLabel, progressBar);
        root.setPadding(new Insets(15));
        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
    }

    public void show() {
        dialogStage.show();
    }

    public void close() {
        dialogStage.close();
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setProgress(double progress) {
        progressBar.setProgress(progress);
    }

}
