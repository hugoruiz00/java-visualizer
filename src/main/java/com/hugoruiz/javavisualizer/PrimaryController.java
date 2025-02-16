package com.hugoruiz.javavisualizer;

import com.hugoruiz.analyzer.CodeAnalysis;
import com.hugoruiz.constants.Constants;
import com.hugoruiz.javavisualizer.modals.Modals;
import com.hugoruiz.models.CodeState;
import com.hugoruiz.models.VariableDetail;
import com.hugoruiz.utils.Utils;

import eu.mihosoft.monacofx.MonacoFX;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import javafx.embed.swing.SwingFXUtils;


public class PrimaryController {
    private List<CodeState> codeStates;
    private List<String> codeExecutionLogs;
    private int currentStep = 0;
    private MonacoFX monacoFX;
    private Timeline timeline;

    @FXML
    private AnchorPane parentAnchorPane;

    @FXML
    private StackPane stackPane;

    @FXML
    private TextArea executionResult, terminal;

    @FXML
    private HBox loadingContainer;

    @FXML
    private Label stepsLabel, loadingLabel;

    @FXML
    private TextField stepField;

    @FXML
    private Button prevBtn, nextBtn, playBtn, createGifBtn;

    @FXML
    public void initialize() {
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        stackPane.getChildren().add(loadingIndicator);
        
        initializeEditor();
        addListenerStepField();
    }

    private void initializeEditor() {
        monacoFX = new MonacoFX();
        monacoFX.getEditor().getDocument().setText(Utils.getInitialStringCode());
        monacoFX.getEditor().setCurrentLanguage("java");
        monacoFX.getEditor().setCurrentTheme("vs-dark");
        Platform.runLater(() -> {
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(event -> {
                stackPane.getChildren().clear();
                stackPane.getChildren().add(monacoFX);
            });
            delay.play();
        });
    }

    private void addListenerStepField() {
        stepField.setOnAction(event -> {
            String text = stepField.getText();
            if (text.matches("^[1-9][0-9]*$")) {
                if(codeStates != null && Integer.parseInt(text) < codeStates.size()) {
                    changeCurrentStep(Integer.parseInt(text) - 1);
                    return;
                }
                changeCurrentStep(codeStates.size() - 1);
                stepField.setText(String.valueOf(codeStates.size()));
                return;
            }
            changeCurrentStep(0);
            stepField.setText("1");
        });

        stepField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("^[0-9]*$")) {
                return change;
            }
            return null;
        }));
    }

    /**
     * Add log of value in return statements
     * Handle args value of System.out.println or System.out.print when there is a \n to do not split or something
     * Add log of class attributes (fields) (outside of methods)
     * Add option to select number of steps for GIF
     */

    // App.setRoot("secondary");
    @FXML
    private void startRunning() {
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingContainer.getChildren().add(loadingIndicator);

        cleanCodeResult();

        Task<Void> task = new Task<>() {
            String executionResult;

            @Override
            protected Void call() throws Exception {
                CodeAnalysis codeAnalysis = new CodeAnalysis();
                String printKey = UUID.randomUUID().toString();
                executionResult = generateCodeExecutionLogs(codeAnalysis, printKey);

                if (executionResult.equals("success")) {
                    codeStates = codeAnalysis.getLogsAsCodeStates(codeExecutionLogs, printKey);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                if (executionResult.equals("success") && !codeStates.isEmpty()) {
                    changeCurrentStep(0);
                    setDisableButtons(false);
                }

                if (!executionResult.equals("success")) {
                    terminal.setText(executionResult);
                }

                loadingContainer.getChildren().remove(loadingIndicator);
                loadingLabel.setText("");
            }
            
            @Override
            protected void failed() {
                super.failed();
                Throwable exception = getException();
                System.err.println("Task failed with exception: " + exception.getMessage());
                terminal.setText("An error ocurred");
                loadingContainer.getChildren().remove(loadingIndicator);
                loadingLabel.setText("");
            }
        };

        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private String generateCodeExecutionLogs(CodeAnalysis codeAnalysis, String printKey) {
        try {
            String stringCode = monacoFX.getEditor().getDocument().getText();
            stringCode = Constants.PACKAGE_DECLARATION + stringCode;
            codeExecutionLogs = codeAnalysis.generateLogsForCodeExecution(stringCode, printKey, Constants.GENERATED_CODE_SOURCE);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @FXML
    void next(ActionEvent event) {
        if(currentStep == codeStates.size() - 1) return;
        stopTimeline();
        changeCurrentStep(++currentStep);
    }

    @FXML
    void prev(ActionEvent event) {
        if(currentStep == 0) return;
        stopTimeline();
        changeCurrentStep(--currentStep);
    }

    @FXML
    void play(ActionEvent event) {
        if (timeline == null || timeline.getStatus() == Timeline.Status.STOPPED) {
            playBtn.setText("Stop");
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (currentStep < codeStates.size() - 1) {
                    changeCurrentStep(++currentStep);
                } else {
                    stopTimeline();
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        } else {
            stopTimeline();
        }
    }

    private void stopTimeline() {
        if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.stop();
        }
        playBtn.setText("Play");
    }

    @FXML
    private void generateGifVisualization() throws IOException {
        stopTimeline();

        String snapshotsFolder = Constants.GENERATED_FILES_SOURCE + "snapshots/";
        if(!Utils.cleanFolder(snapshotsFolder)) {
            return;
        }
        
        List<String> snapshots = new ArrayList<>();
        boolean[] gifCreated = new boolean[1];

        Stage ownerStage = (Stage) parentAnchorPane.getScene().getWindow();
        Stage modal = Modals.createModalCreatingGif(ownerStage, "Capturing", "Capturing, please wait...");

        Task<Void> snapshotTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                takeSnapshots(snapshots, snapshotsFolder);

                try {
                    String outputGifPath = Constants.GENERATED_FILES_SOURCE + "animation.gif";
                    Utils.createGif(snapshots, outputGifPath, 1000);
                    gifCreated[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
    
                return null;
            }
    
            @Override
            protected void succeeded() {
                super.succeeded();
                Platform.runLater(modal::close);
            }
    
            @Override
            protected void failed() {
                super.failed();
                Platform.runLater(modal::close);
                Throwable exception = getException();
                exception.printStackTrace();
            }
        };

        modal.setOnHidden(event -> {
            modalClosed(snapshotTask, ownerStage, gifCreated[0]);
        });
        modal.show();
    
        Thread thread = new Thread(snapshotTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void takeSnapshots(List<String> snapshots, String snapshotsFolder) throws InterruptedException {
        List<String> tempSnapshots = new ArrayList<>();
        
        for (int i = 0; i < codeStates.size(); i++) {
            final int step = i;

            Platform.runLater(() -> changeCurrentStep(step));
            Thread.sleep(500);

            Platform.runLater(() -> {
                WritableImage snapshot = parentAnchorPane.getScene().snapshot(null);
                File file = new File(snapshotsFolder + step + "snapshot.png");

                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                    tempSnapshots.add(file.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to save snapshot as image.");
                }
            });
        }

        snapshots.addAll(tempSnapshots);
    }

    private void modalClosed(Task<Void> snapshotTask, Stage ownerStage, boolean gifCreated) {
        snapshotTask.cancel();
        try {
            if(gifCreated) {
                Stage modalCreated = Modals.createModalGifCreated(ownerStage, Constants.GENERATED_FILES_SOURCE);
                modalCreated.show();
            } else {
                Stage modalError = Modals.createModalError(ownerStage, "GIF not created", "Could not create the GIF");
                modalError.show();
            } 
        } catch (IOException e) {}
    }
    
    private void cleanCodeResult() {
        stopTimeline();

        terminal.setText("");
        stepField.setText("0");
        stepsLabel.setText("of 0");
        executionResult.setText("");
        loadingLabel.setText("Loading");
        codeStates = new ArrayList<CodeState>();
        setDisableButtons(true);
    }

    private void setDisableButtons(boolean disable) {
        prevBtn.setDisable(disable);
        nextBtn.setDisable(disable);
        playBtn.setDisable(disable);
        stepField.setDisable(disable);
        createGifBtn.setDisable(disable);
    }

    private void changeCurrentStep(int newStep) {
        currentStep = newStep;
        updateView();
        selectLineInEditor(codeStates.get(currentStep).getLineNumber());
    }

    private void selectLineInEditor(int line){
        WebEngine webEngine = monacoFX.getWebEngine();
        String jsScript = String.format(
            "editorView.setSelection({ startLineNumber: %d, startColumn: 1, endLineNumber: %d, endColumn: 1 });", 
            line, 
            (line+1)
        );
        webEngine.executeScript(jsScript);
        monacoFX.getEditor().getViewController().scrollToLineCenter(line);
    }

    private void updateView() {
        Map<String, VariableDetail> currentStepVariables = codeStates.get(currentStep).getVariables();
        executionResult.setText(mapToString(currentStepVariables));
        stepField.setText((currentStep+1)+"");
        stepsLabel.setText(" of " + codeStates.size());
        terminal.setText(codeStates.get(currentStep).getChangeDescription());
    }

    private String mapToString(Map<String, VariableDetail> map) {
        Map<String, List<VariableDetail>> groupedByMethod = groupByMethod(map);

        StringBuilder mapAsString = new StringBuilder("");
        groupedByMethod.forEach((method, variables) -> {
            mapAsString.append("Method [" + method + "]" + "\n");
            variables.forEach(var -> {
                mapAsString.append(var.getName() + " = " + var.getValue() + "\n");
            });
            mapAsString.append("\n");
        });

        return mapAsString.toString();
    }

    private static Map<String, List<VariableDetail>> groupByMethod(Map<String, VariableDetail> variableMap) {
        return variableMap.values().stream().collect(Collectors.groupingBy(VariableDetail::getMethod));
    }
}
