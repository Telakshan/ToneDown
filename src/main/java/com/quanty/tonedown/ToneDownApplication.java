package com.quanty.tonedown;

import com.quanty.tonedown.utils.CensorService;
import com.quanty.tonedown.utils.DependencyUtils;
import com.quanty.tonedown.utils.ProcessAudio;
import com.quanty.tonedown.utils.ProcessLanguage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ToneDownApplication extends Application {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".mp4", ".mov", "wav");

    private VBox dropView;
    private VBox infoView;
    private VBox progressView;
    private StackPane mainContainer;

    private Label fileNameLabel;
    private Label fileLengthLabel;
    private Label fileFormatLabel;
    private Label extractTimeLabel;
    private Label transcribeTimeLabel;
    private Label totalTimeLabel;

    private ProgressBar mainProgressBar;
    private Label statusLabel;
    private TextArea logConsole;
    private Button proceedButton;

    private File currentFile;
    private long videoDurationMillis;

    @Override
    public void start(Stage primaryStage) {
        redirectSystemStreams();

        mainContainer = new StackPane();
        mainContainer.setPadding(new Insets(20));

        createDropView();

        createInfoView();

        createProgressView();

        mainContainer.getChildren().addAll(dropView, infoView, progressView);
        showView(dropView);

        Scene scene = new Scene(mainContainer, 600, 500);

        try {
            String css = Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load style.css: " + e.getMessage());
        }

        primaryStage.setTitle("ToneDown - AI Profanity Muter");
        primaryStage.setScene(scene);
        primaryStage.show();

        checkDependencies();
    }

    private void createDropView() {
        dropView = new VBox(20);
        dropView.setAlignment(Pos.CENTER);
        dropView.getStyleClass().add("drag-target");

        Text icon = new Text("📂");
        icon.setStyle("-fx-font-size: 48px;");

        Text title = new Text("Drag & Drop Video File");
        title.getStyleClass().add("header-text");

        Text subtitle = new Text("Supports MP4, MOV, WAV");
        subtitle.getStyleClass().add("sub-header");

        dropView.getChildren().addAll(icon, title, subtitle);

        dropView.setOnDragOver(event -> {
            if (event.getGestureSource() != dropView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropView.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().get(0);
                if (isValidFile(file)) {
                    analyzeFile(file);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void createInfoView() {
        infoView = new VBox(15);
        infoView.setAlignment(Pos.CENTER_LEFT);
        infoView.setVisible(false);

        Text header = new Text("File Analysis");
        header.getStyleClass().add("header-text");

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(10);

        int r = 0;
        detailsGrid.add(createDetailLabel("File Name:"), 0, r);
        fileNameLabel = createDetailValue("...");
        detailsGrid.add(fileNameLabel, 1, r++);

        detailsGrid.add(createDetailLabel("Duration:"), 0, r);
        fileLengthLabel = createDetailValue("...");
        detailsGrid.add(fileLengthLabel, 1, r++);

        detailsGrid.add(createDetailLabel("Format:"), 0, r);
        fileFormatLabel = createDetailValue("...");
        detailsGrid.add(fileFormatLabel, 1, r++);

        Separator sep = new Separator();

        GridPane estGrid = new GridPane();
        estGrid.setHgap(20);
        estGrid.setVgap(10);

        r = 0;
        estGrid.add(createDetailLabel("Est. Extraction Time:"), 0, r);
        extractTimeLabel = createDetailValue("...");
        estGrid.add(extractTimeLabel, 1, r++);

        estGrid.add(createDetailLabel("Est. Transcription Time:"), 0, r);
        transcribeTimeLabel = createDetailValue("...");
        estGrid.add(transcribeTimeLabel, 1, r++);

        estGrid.add(createDetailLabel("Total Est. Time:"), 0, r);
        totalTimeLabel = createDetailValue("...");
        totalTimeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0078d7;");
        estGrid.add(totalTimeLabel, 1, r++);

        proceedButton = new Button("Start Processing");
        proceedButton.getStyleClass().add("button");
        proceedButton.setPrefWidth(200);
        proceedButton.setScaleX(1.2);
        proceedButton.setScaleY(1.2);
        proceedButton.setOnAction(e -> startProcessing());

        Button backButton = new Button("Cancel");
        backButton.setOnAction(e -> showView(dropView));

        HBox buttons = new HBox(20, backButton, proceedButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        infoView.getChildren().addAll(header, detailsGrid, sep, estGrid, buttons);
    }

    private void createProgressView() {
        progressView = new VBox(15);
        progressView.setAlignment(Pos.TOP_LEFT);
        progressView.setVisible(false);

        Text header = new Text("Processing...");
        header.getStyleClass().add("header-text");

        statusLabel = new Label("Initializing...");
        statusLabel.setStyle("-fx-font-size: 14px;");

        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(Double.MAX_VALUE);

        logConsole = new TextArea();
        logConsole.setEditable(false);
        logConsole.setWrapText(true);
        logConsole.setPrefHeight(300);

        progressView.getChildren().addAll(header, statusLabel, mainProgressBar, new Label("Console Logs:"), logConsole);
    }

    private void analyzeFile(File file) {
        this.currentFile = file;
        showView(infoView);

        fileNameLabel.setText(file.getName());
        fileFormatLabel.setText(getFileExtension(file).toUpperCase());
        fileLengthLabel.setText("Calculating...");

        new Thread(() -> {
            long duration = ProcessAudio.getDuration(file.toPath());
            this.videoDurationMillis = duration;

            Platform.runLater(() -> {
                String durationStr = formatDuration(duration);
                fileLengthLabel.setText(durationStr);

                long extractTime = duration / 20;
                long transcribeTime = duration / 5;

                extractTimeLabel.setText("~" + formatDuration(extractTime));
                transcribeTimeLabel.setText("~" + formatDuration(transcribeTime));
                totalTimeLabel.setText("~" + formatDuration(extractTime + transcribeTime));
            });
        }).start();
    }

    private void startProcessing() {
        showView(progressView);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                updateMessage("Checking dependencies...");
                if (!DependencyUtils.isModelAvailable()) {
                    updateMessage("Downloading AI Model (This happens once)...");
                    DependencyUtils.downloadModel(progress -> updateProgress(progress, 1.0));
                }

                File audioFile = getAudioFile(currentFile);
                updateMessage("Extracting Audio...");
                ProcessAudio.extractAudio(currentFile.toPath(), audioFile.toPath(), p -> {
                    updateProgress(p * 0.3, 1.0);
                });

                Path modelPath = DependencyUtils.getModelPath();

                updateProgress(0.4, 1.0);
                Path transcript = ProcessLanguage.runWhisper(modelPath, audioFile.toPath());
                updateProgress(0.6, 1.0);

                File outputFile = new File(currentFile.getParent(), "tonedown_" + currentFile.getName());

                Gson gson = new Gson();
                List<ProcessLanguage.TranscriptSegment> segments = gson.fromJson(
                        java.nio.file.Files.readString(transcript),
                        new TypeToken<List<ProcessLanguage.TranscriptSegment>>() {
                        }.getType());

                CensorService.censorVideo(currentFile.toPath(), outputFile.toPath(), segments, audioFile.toPath(),
                        modelPath, p -> {
                            updateProgress(0.6 + (p * 0.4), 1.0);
                        });

                updateMessage("Processing Complete. Output at: " + outputFile.getName());
                updateProgress(1.0, 1.0);
                /* 
                updateMessage("Verifying Censorship...");
                try {
                    File outputAudio = new File(currentFile.getParent(),
                            "tonedown_verify_" + currentFile.getName() + ".wav");
                    ProcessAudio.extractAudio(outputFile.toPath(), outputAudio.toPath(), null);

                    Path verifyTranscriptPath = ProcessLanguage.runWhisper(modelPath, outputAudio.toPath());

                    List<ProcessLanguage.TranscriptSegment> verifySegments = gson.fromJson(
                            java.nio.file.Files.readString(verifyTranscriptPath),
                            new TypeToken<List<ProcessLanguage.TranscriptSegment>>() {
                            }.getType());

                    int badWordsFound = 0;
                    for (ProcessLanguage.TranscriptSegment seg : verifySegments) {
                        String text = seg.text.toLowerCase();
                        for (String badWord : CensorService.BAD_WORDS) {
                            if (text.contains(badWord)) {
                                badWordsFound++;
                                System.out.println("Verification Failed: Found '" + badWord + "' in segment: " + text);
                            }
                        }
                    }

                    final int finalBadWords = badWordsFound;
                    Platform.runLater(() -> {
                        if (finalBadWords == 0) {
                            statusLabel.setText("Verification PASSED: Clean Audio.");
                            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else {
                            statusLabel.setText("Verification FAILED: " + finalBadWords + " words remaining.");
                            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        }
                    }); 

                    outputAudio.delete();

                } catch (Exception e) {
                    System.err.println("Verification failed due to error: " + e.getMessage());
                    e.printStackTrace();
                }*/

                return null;
            }
        };

    task.messageProperty().addListener((obs,old,msg)->statusLabel.setText(msg));mainProgressBar.progressProperty().bind(task.progressProperty());

    task.setOnFailed(e->

    {
        Throwable ex = task.getException();
        statusLabel.setText("Error: " + ex.getMessage());
        ex.printStackTrace();
        mainProgressBar.setStyle("-fx-accent: red;");
    });

    task.setOnSucceeded(e->
    {
        statusLabel.setText("Done! File processed.");
        mainProgressBar.setStyle("-fx-accent: green;");
    });

    new Thread(task).start();
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                appendText(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                appendText(new String(b, off, len));
            }

            private void appendText(String text) {
                Platform.runLater(() -> {
                    if (logConsole != null)
                        logConsole.appendText(text);
                });
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void showView(VBox view) {
        dropView.setVisible(view == dropView);
        dropView.setManaged(view == dropView);
        infoView.setVisible(view == infoView);
        infoView.setManaged(view == infoView);
        progressView.setVisible(view == progressView);
        progressView.setManaged(view == progressView);
    }

    private Label createDetailLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("detail-label");
        return l;
    }

    private Label createDetailValue(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("detail-value");
        return l;
    }

    private File getAudioFile(File file) {
        String originalName = file.getName();
        String nameWithoutExtension = originalName.substring(0, originalName.lastIndexOf("."));
        return new File(file.getParent(), nameWithoutExtension + "_audio.wav");
    }

    private boolean isValidFile(File file) {
        String fileName = file.getName().toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1)
            return "";
        return name.substring(lastIndexOf + 1);
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%d min %d sec", minutes, seconds);
    }

    private void checkDependencies() {
        new Thread(() -> {
            if (!DependencyUtils.isModelAvailable()) {
                System.out.println("Wait! AI Model missing. It will be downloaded when you start processing.");
            } else {
                System.out.println("System Ready. Dependencies found.");
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
