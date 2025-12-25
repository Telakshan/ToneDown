package com.quanty.tonedown.utils;

import com.github.kokorin.jaffree.ffmpeg.*;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;

import java.nio.file.Path;

public class ProcessAudio {

    public static long getDuration(Path inputVideo) {
        try {
            FFprobeResult probeResult = FFprobe.atPath()
                    .setShowStreams(true)
                    .setInput(inputVideo)
                    .execute();

            return (long) (probeResult.getStreams().get(0).getDuration() * 1000);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void extractAudio(Path inputVideo, Path outputAudio,
            java.util.function.Consumer<Double> progressCallback) {
        System.out.println("Starting Audio Extraction...");
        long totalDurationMillis = getDuration(inputVideo);

        // We need 16kHz mono 16-bit WAV for Whisper
        FFmpeg.atPath()
                .addInput(UrlInput.fromPath(inputVideo))
                .addOutput(UrlOutput.toPath(outputAudio)
                        .setFormat("wav")
                        .addArguments("-ac", "1")
                        .addArguments("-ar", "16000"))
                .setOverwriteOutput(true)
                .setProgressListener(progress -> {
                    if (totalDurationMillis > 0 && progressCallback != null) {
                        double percent = (double) progress.getTimeMillis() / totalDurationMillis;
                        progressCallback.accept(percent);
                    }
                })
                .execute();

        System.out.println("Audio extracted successfully.");
    }

    public static void extractClip(Path inputFile, Path outputFile, double startSeconds, double durationSeconds) {
        FFmpeg.atPath()
                .addInput(UrlInput.fromPath(inputFile))
                .addOutput(UrlOutput.toPath(outputFile))
                .setOverwriteOutput(true)
                .addArguments("-ss", String.format(java.util.Locale.US, "%.3f", startSeconds))
                .addArguments("-t", String.format(java.util.Locale.US, "%.3f", durationSeconds))
                .execute();
    }
}
