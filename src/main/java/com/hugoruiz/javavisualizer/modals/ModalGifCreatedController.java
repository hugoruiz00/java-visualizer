package com.hugoruiz.javavisualizer.modals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

public class ModalGifCreatedController {

  @FXML
  private VBox container;

  private String gifPath;

  public void setGifPath(String gifPath) {
    this.gifPath = gifPath;
  }

  @FXML
  private void saveGif() {
    FileChooser fileChooser = new FileChooser();
    
    fileChooser.setTitle("Save GIF");
    fileChooser.setInitialFileName("animation.gif");
    
    File saveFile = fileChooser.showSaveDialog(null);

    if (saveFile != null) {
      try {
        Files.copy(Paths.get(gifPath), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        addLabel("Saved at " + saveFile.toPath());
      } catch (IOException e) {
        addLabel("Could not save GIF");
      }
    }
  }

  private void addLabel(String text) {
    Label resultLabel = new Label(text);
    resultLabel.setStyle("-fx-text-fill: white;");
    resultLabel.setWrapText(true);
    resultLabel.setTextAlignment(TextAlignment.CENTER);

    if (container.getChildren().size() > 2) {
      container.getChildren().set(2, resultLabel);
    } else {
      container.getChildren().add(2, resultLabel);
    }
  }
}
