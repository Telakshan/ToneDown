package com.quanty.tonedown.utils;

import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class ProcessLanguage {

    private static boolean libraryLoaded = false;

    public static Path runWhisper(Path whisperExe, Path modelPath, Path audioFile) throws IOException {
        System.out.println("Transcribing with WhisperJNI...");

        if (!libraryLoaded) {
            WhisperJNI.loadLibrary();
            WhisperJNI.setLibraryLogger(null);
            libraryLoaded = true;
        }

        WhisperJNI whisper = new WhisperJNI();
        WhisperContext ctx = null;

        try {
            ctx = whisper.init(modelPath);

            // Read audio file into float array
            float[] samples = readAudioSamples(audioFile);

            WhisperFullParams params = new WhisperFullParams();
            // Optional: configure params
            params.printProgress = false;

            int result = whisper.full(ctx, params, samples, samples.length);
            if (result != 0) {
                throw new IOException("Transcription failed with code " + result);
            }

            int numSegments = whisper.fullNSegments(ctx);
            List<TranscriptSegment> segments = new ArrayList<>();

            for (int i = 0; i < numSegments; i++) {
                long t0 = whisper.fullGetSegmentTimestamp0(ctx, i);
                long t1 = whisper.fullGetSegmentTimestamp1(ctx, i);
                String text = whisper.fullGetSegmentText(ctx, i);
                segments.add(new TranscriptSegment(t0, t1, text));
            }

            Path outputDir = Path.of("bin", "temp");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Path resultPath = outputDir.resolve("transcript.json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(resultPath.toFile())) {
                gson.toJson(segments, writer);
            }

            System.out.println("Transcription complete: " + resultPath + " created.");
            return resultPath;

        } catch (Exception e) {
            throw new IOException("Error during transcription", e);
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    private static float[] readAudioSamples(Path audioFile) throws IOException, UnsupportedAudioFileException {

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile.toFile())) {
            AudioFormat format = ais.getFormat();

            if (format.getSampleRate() != 16000 || format.getChannels() != 1) {
                AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);
                AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, ais);
                return readSamples(converted);
            } else {
                return readSamples(ais);
            }
        }
    }

    private static float[] readSamples(AudioInputStream ais) throws IOException {
        byte[] bytes = ais.readAllBytes();
        ShortBuffer sbuf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        float[] samples = new float[sbuf.capacity()];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = sbuf.get(i) / 32768.0f;
        }
        return samples;
    }

    public static class TranscriptSegment {
        public long start;
        public long end;
        public String text;

        public TranscriptSegment(long start, long end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }

        public double getStartSeconds() {
            return start / 100.0;
        }

        public double getEndSeconds() {
            return end / 100.0;
        }
    }
}
