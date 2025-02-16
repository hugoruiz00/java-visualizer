package com.hugoruiz.javavisualizer.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ModalGifCreatingController {
  @FXML
  private Label messageLabel;

  public void setMessage(String message) {
    messageLabel.setText(message);
  }
}
