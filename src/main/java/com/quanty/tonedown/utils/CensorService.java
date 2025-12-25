package com.quanty.tonedown.utils;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class CensorService {

    public static final List<String> BAD_WORDS = Arrays.asList("fuck", "shit", "bitch", "ass", "crap");

    public static List<String> generateMuteFilters(List<ProcessLanguage.TranscriptSegment> segments, Path originalAudio,
            Path modelPath) {
        List<String> muteFilters = new ArrayList<>();
        Gson gson = new Gson();

        for (ProcessLanguage.TranscriptSegment seg : segments) {
            String text = seg.text.toLowerCase();
            for (String badWord : BAD_WORDS) {
                if (text.contains(badWord)) {
                    System.out.println("Refining timestamp for: " + badWord + " in segment: " + seg.text);

                    double startSeconds = seg.getStartSeconds();
                    double endSeconds = seg.getEndSeconds();
                    double duration = endSeconds - startSeconds;

                    if (originalAudio == null || modelPath == null) {
                        muteFilters
                                .add(String.format("volume=0:enable='between(t,%.3f,%.3f)'", startSeconds, endSeconds));
                        continue;
                    }

                    try {
                        Path tempClip = Path.of("bin", "temp", "clip_" + System.currentTimeMillis() + ".wav");
                        ProcessAudio.extractClip(originalAudio, tempClip, startSeconds, duration);

                        double refinedOffsetStart = 0;
                        for (double offset = 0; offset < duration - 0.2; offset += 0.2) {
                            Path subClip = Path.of("bin", "temp", "sub_" + System.currentTimeMillis() + ".wav");
                            ProcessAudio.extractClip(tempClip, subClip, offset, duration - offset);

                            Path result = ProcessLanguage.runWhisper(null, modelPath, subClip);
                            List<ProcessLanguage.TranscriptSegment> subSegs = gson.fromJson(
                                    java.nio.file.Files.readString(result),
                                    new TypeToken<List<ProcessLanguage.TranscriptSegment>>() {
                                    }.getType());

                            boolean stillHasWord = false;
                            for (ProcessLanguage.TranscriptSegment s : subSegs) {
                                if (s.text.toLowerCase().contains(badWord))
                                    stillHasWord = true;
                            }

                            Files.deleteIfExists(subClip);
                            Files.deleteIfExists(result);

                            if (!stillHasWord) {
                                refinedOffsetStart = Math.max(0, offset - 0.2);
                                break;
                            }
                            refinedOffsetStart = offset;
                        }

                        double refinedOffsetEnd = duration;
                        for (double offset = duration; offset > refinedOffsetStart + 0.2; offset -= 0.2) {
                            Path subClip = Path.of("bin", "temp", "sub_end_" + System.currentTimeMillis() + ".wav");
                            ProcessAudio.extractClip(tempClip, subClip, refinedOffsetStart,
                                    offset - refinedOffsetStart);

                            Path result = ProcessLanguage.runWhisper(null, modelPath, subClip);
                            List<ProcessLanguage.TranscriptSegment> subSegs = gson.fromJson(
                                    java.nio.file.Files.readString(result),
                                    new TypeToken<List<ProcessLanguage.TranscriptSegment>>() {
                                    }.getType());

                            boolean stillHasWord = false;
                            for (ProcessLanguage.TranscriptSegment s : subSegs) {
                                if (s.text.toLowerCase().contains(badWord))
                                    stillHasWord = true;
                            }

                            Files.deleteIfExists(subClip);
                            Files.deleteIfExists(result);

                            if (stillHasWord) {
                                refinedOffsetEnd = offset;
                            } else {
                                refinedOffsetEnd = offset + 0.2;
                                break;
                            }
                            refinedOffsetEnd = offset;
                        }

                        Files.deleteIfExists(tempClip);

                        startSeconds = seg.getStartSeconds() + refinedOffsetStart;
                        endSeconds = seg.getStartSeconds() + refinedOffsetEnd;

                        startSeconds = Math.max(0, startSeconds - 0.05);
                        endSeconds = endSeconds + 0.05;

                        System.out.println("Refined Mute: " + startSeconds + " -> " + endSeconds);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    muteFilters.add(String.format("volume=0:enable='between(t,%.3f,%.3f)'", startSeconds, endSeconds));
                }
            }
        }
        return muteFilters;
    }

    public static void censorVideo(Path inputVideo, Path outputVideo, List<ProcessLanguage.TranscriptSegment> segments,
            Path originalAudio, Path modelPath,
            java.util.function.Consumer<Double> progressCallback) {

        System.out.println("Censoring video...");

        List<String> muteFilters = generateMuteFilters(segments, originalAudio, modelPath);

        if (muteFilters.isEmpty()) {
            System.out.println("No bad words found. Copying video.");
            FFmpeg.atPath()
                    .addInput(UrlInput.fromPath(inputVideo))
                    .addOutput(UrlOutput.toPath(outputVideo))
                    .setOverwriteOutput(true)
                    .execute();
            return;
        }

        String audioFilter = String.join(",", muteFilters);

        long totalDurationMillis = ProcessAudio.getDuration(inputVideo);

        FFmpeg.atPath()
                .addInput(UrlInput.fromPath(inputVideo))
                .addOutput(UrlOutput.toPath(outputVideo)
                        .setCodec(StreamType.VIDEO, "copy")
                        .addArguments("-af", audioFilter))
                .setOverwriteOutput(true)
                .setProgressListener(progress -> {
                    if (totalDurationMillis > 0 && progressCallback != null) {
                        double percent = (double) progress.getTimeMillis() / totalDurationMillis;
                        progressCallback.accept(percent);
                    }
                })
                .execute();

        System.out.println("Video censored successfully.");
    }
}
