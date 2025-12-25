package com.quanty.tonedown;

import com.quanty.tonedown.utils.CensorService;
import com.quanty.tonedown.utils.ProcessLanguage;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CensorServiceTest {

    @Test
    public void testLinearInterpolationLogic() {

        ProcessLanguage.TranscriptSegment seg = new ProcessLanguage.TranscriptSegment(100, 300, "What the fuck?");

        List<String> filters = CensorService.generateMuteFilters(Collections.singletonList(seg), null, null);

        System.out.println("Generated Filters: " + filters);

        assertTrue(filters.size() > 0);
        String filter = filters.get(0);

        assertTrue(filter.contains("volume=0"));
    }

    @Test
    public void testAbsolutelyFuckingNot() {

        ProcessLanguage.TranscriptSegment seg = new ProcessLanguage.TranscriptSegment(0, 200, "Absolutely fucking not");

        List<String> filters = CensorService.generateMuteFilters(Collections.singletonList(seg), null, null);
        System.out.println("Generated Filters (Abs): " + filters);

    }

    @Test
    public void testEarlyWord() {

        ProcessLanguage.TranscriptSegment seg = new ProcessLanguage.TranscriptSegment(0, 100, "shit happens");
        List<String> filters = CensorService.generateMuteFilters(Collections.singletonList(seg), null, null);
        System.out.println("Generated Filters (Start): " + filters);

    }
}
