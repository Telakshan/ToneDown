package com.quanty.tonedown;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;

public class ToneDownController {
    @FXML
    private Label fileName;

    @FXML
    protected void onDragOver(DragEvent event) { fileName.setText(String.format("%s was dragged over", event.getDragboard().getFiles())); }
}
