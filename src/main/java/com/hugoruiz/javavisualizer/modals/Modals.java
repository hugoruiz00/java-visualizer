package com.hugoruiz.javavisualizer.modals;

import java.io.IOException;

import com.hugoruiz.javavisualizer.App;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Modals {
  public static Stage createModalGifCreated(Stage ownerStage, String sourceFile) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("modals/ModalGifCreated.fxml"));
    VBox modalContent = loader.load();

    ModalGifCreatedController controller = loader.getController();
    controller.setGifPath(sourceFile + "animation.gif");

    Stage modalStage = new Stage();
    modalStage.initOwner(ownerStage);
    modalStage.initModality(Modality.APPLICATION_MODAL);
    modalStage.setScene(new Scene(modalContent));
    modalStage.setTitle("GIF created");

    return modalStage;
  }

  public static Stage createModalCreatingGif(Stage ownerStage, String title, String message) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("modals/ModalGifCreating.fxml"));
    VBox modalContent = loader.load();

    ModalGifCreatingController controller = loader.getController();
    controller.setMessage(message);

    Stage modalStage = new Stage();
    modalStage.initOwner(ownerStage);
    modalStage.initModality(Modality.APPLICATION_MODAL);
    modalStage.setScene(new Scene(modalContent));
    modalStage.setTitle(title);

    return modalStage;
  }

  public static Stage createModalError(Stage ownerStage, String error, String description) throws IOException {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("modals/ModalError.fxml"));
    VBox modalContent = loader.load();

    ModalErrorController controller = loader.getController();
    controller.setError(error);
    controller.setDescription(description);

    Stage modalStage = new Stage();
    modalStage.initOwner(ownerStage);
    modalStage.initModality(Modality.APPLICATION_MODAL);
    modalStage.setScene(new Scene(modalContent));
    modalStage.setTitle("Error");

    return modalStage;
  }
}
