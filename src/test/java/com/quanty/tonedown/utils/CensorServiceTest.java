package com.quanty.tonedown.utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class CensorServiceTest {

    @Test
    public void testGenerateMuteFilters() {
        List<ProcessLanguage.TranscriptSegment> segments = new ArrayList<>();
        segments.add(new ProcessLanguage.TranscriptSegment(0, 100, "hello"));
        segments.add(new ProcessLanguage.TranscriptSegment(100, 200, "fuck you"));
        segments.add(new ProcessLanguage.TranscriptSegment(200, 300, "world"));
        segments.add(new ProcessLanguage.TranscriptSegment(300, 400, "eat shit"));

        List<String> muteFilters = CensorService.generateMuteFilters(segments, null, null);

        assertEquals(2, muteFilters.size());
        assertEquals("volume=0:enable='between(t,1.000,2.000)'", muteFilters.get(0));
        assertEquals("volume=0:enable='between(t,3.000,4.000)'", muteFilters.get(1));
    }

    @Test
    public void testNoBadWords() {
        List<ProcessLanguage.TranscriptSegment> segments = new ArrayList<>();
        segments.add(new ProcessLanguage.TranscriptSegment(0, 100, "hello world"));

        List<String> muteFilters = CensorService.generateMuteFilters(segments, null, null);

        assertTrue(muteFilters.isEmpty());
    }
}
