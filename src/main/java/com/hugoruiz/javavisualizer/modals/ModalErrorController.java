package com.hugoruiz.javavisualizer.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ModalErrorController {
  @FXML
  private Label errorLabel, descriptionLabel;

  public void setError(String error) {
    errorLabel.setText(error);
  }

  public void setDescription(String description) {
    descriptionLabel.setText(description);
  }
}
