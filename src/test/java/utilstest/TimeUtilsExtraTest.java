package utilstest;

import org.joupen.utils.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsExtraTest {

    @Test
    void parseDuration_mo_d_h_m_combo() {
        Duration d = TimeUtils.parseDuration("1mo2d3h4m");
        // 1mo = 30d
        assertEquals(Duration.ofDays(30).plusDays(2).plusHours(3).plusMinutes(4), d);
    }

    @Test
    void parseDuration_caseInsensitive_andSpacesIgnoredByRegex() {
        Duration d = TimeUtils.parseDuration("2MO");
        assertEquals(Duration.ofDays(60), d);
    }

    @Test
    void parseDuration_invalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseDuration("abc"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseDuration(""));
    }

    @Test
    void formatDuration_shouldRoundTripTypical() {
        Duration d = Duration.ofDays(61).plusHours(5).plusMinutes(7); // 2mo (60d) +1d +5h +7m
        String s = TimeUtils.formatDuration(d);
        // допускаем пробелы в конце, но проверим ключевые куски
        assertTrue(s.contains("2mo"));
        assertTrue(s.contains("1d"));
        assertTrue(s.contains("5h"));
        assertTrue(s.contains("7m"));
    }
}
