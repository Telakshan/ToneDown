package com.quanty.tonedown.utils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependencyUtils {

    private static final Path APP_DATA_DIR = Paths.get(System.getProperty("user.home"), ".tonedown");
    private static final Path MODELS_DIR = APP_DATA_DIR.resolve("models");

    private static final String MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin";
    private static final String MODEL_FILENAME = "ggml-base.en.bin";

    public static Path getModelPath() {
        return MODELS_DIR.resolve(MODEL_FILENAME);
    }

    public static boolean isModelAvailable() {
        return Files.exists(getModelPath()) && Files.isRegularFile(getModelPath());
    }

    public static void downloadModel(java.util.function.Consumer<Double> progressCallback) throws IOException {
        if (isModelAvailable())
            return;

        Files.createDirectories(MODELS_DIR);
        Path targetPath = getModelPath();

        URL url = URI.create(MODEL_URL).toURL();
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toFile())) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            long fileSize = url.openConnection().getContentLengthLong();

            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (progressCallback != null && fileSize > 0) {
                    progressCallback.accept((double) totalBytesRead / fileSize);
                }
            }
        }
    }
}
