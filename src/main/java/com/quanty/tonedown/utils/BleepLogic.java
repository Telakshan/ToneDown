package com.quanty.tonedown.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class BleepLogic {
    class Transcript { List<Segment> transcription; }
    class Segment { List<Token> tokens; }
    class Token { String text; long t0; long t1; }

    public static String generateMuteFilter(String jsonPath, List<String> badWords) throws FileNotFoundException {
        Gson gson = new Gson();
        Transcript data = gson.fromJson(new FileReader(jsonPath), Transcript.class);

        StringBuilder filter = new StringBuilder("volume=0:enable='");
        boolean first = true;

        for (Segment seg : data.transcription){
            for(Token token : seg.tokens) {

                String word = token.text.trim().toLowerCase().replaceAll("[^a-z]", "");

                if (badWords.contains(word)) {
                    if (!first) filter.append("+");

                    double start = token.t0 / 100.0;
                    double end = token.t1 / 100.0;

                    filter.append("between(t,").append(start).append(",").append(end).append(")");
                    first = false;
                }
            }
        }

        return filter.toString();
    }
}
