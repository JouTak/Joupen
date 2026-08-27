package utilstest;

import org.joupen.utils.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest {

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
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseDuration("-3d"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseDuration("3days"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseDuration("3d-junk"));
    }

    @Test
    void adjustmentsPreserveSign() {
        assertEquals(Duration.ofDays(-3), TimeUtils.parseAdjustment("-3d"));
        assertEquals(Duration.ofHours(12), TimeUtils.parseAdjustment("+12h"));
        assertEquals("-3d", TimeUtils.formatDuration(Duration.ofDays(-3)));
    }

    @Test
    void overflowingAmountsCannotChangeSign() {
        assertEquals(Duration.ofMinutes(4294967294L), TimeUtils.parseDuration("2147483647m2147483647m"));
        assertThrows(ArithmeticException.class, () -> TimeUtils.parseAdjustment("-9223372036854775807d"));
        assertThrows(ArithmeticException.class, () -> TimeUtils.parseAdjustment("9223372036854775807m1m"));
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
