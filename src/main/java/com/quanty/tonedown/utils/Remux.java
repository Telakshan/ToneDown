package com.quanty.tonedown.utils;

import java.io.IOException;
import java.nio.file.Path;

public class Remux {
    public static void createBleepedVideo(Path inputVideo, String filterString, Path outputVideo) throws IOException, InterruptedException {
        System.out.println("Bleeping...");

        new ProcessBuilder(
                "ffmpeg",
                "-i", inputVideo.toString(),
                "-af", filterString,
                "-c:v", "copy",
                "-c:a", "aac",
                outputVideo.toString()
        ).inheritIO().start().waitFor();

        System.out.println("Done! Saved to: " + outputVideo);
    }
}
