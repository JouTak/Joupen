package org.joupen.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class TimeUtils {

    private TimeUtils() {
        // utility class
    }

    public static Duration parseDuration(String durationStr) {
        log.info("Parsing duration string: {}", durationStr);

        if (durationStr == null || durationStr.isBlank()) {
            throw new IllegalArgumentException("Duration string is blank");
        }

        // mo должно идти раньше m, иначе "mo" распарсится как "m"
        Pattern pattern = Pattern.compile("(\\d+)(mo|[dhm])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(durationStr.toLowerCase());

        int days = 0;
        int hours = 0;
        int minutes = 0;
        int months = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "mo":
                    months += value;   // += чтобы можно было "1mo2mo"
                    log.info("Parsed months += {}", value);
                    break;
                case "d":
                    days += value;
                    log.info("Parsed days += {}", value);
                    break;
                case "h":
                    hours += value;
                    log.info("Parsed hours += {}", value);
                    break;
                case "m":
                    minutes += value;
                    log.info("Parsed minutes += {}", value);
                    break;
                default:
                    // сюда не попадём из-за regex, но пусть будет
                    throw new IllegalArgumentException("Unknown duration unit: " + unit);
            }
        }

        if (!found) {
            log.warn("No valid duration found in string: {}", durationStr);
            throw new IllegalArgumentException("Invalid duration format: " + durationStr);
        }

        Duration duration = Duration.ofDays(months * 30L)
                .plusDays(days)
                .plusHours(hours)
                .plusMinutes(minutes);

        log.info("Parsed duration result: {} (months={}, days={}, hours={}, minutes={})",
                duration, months, days, hours, minutes);

        return duration;
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "0m";
        }

        long totalDays = duration.toDays();
        long months = totalDays / 30;
        long days = totalDays % 30;
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (months > 0) sb.append(months).append("mo ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");

        String result = sb.toString().trim();
        log.info("Formatted duration: {}", result);
        return result.isEmpty() ? "0m" : result;
    }

    public static PassProgress calculatePassProgress(
            LocalDateTime now,
            LocalDateTime lastProlong,
            LocalDateTime validUntil
    ) {
        if (now == null || lastProlong == null || validUntil == null) {
            throw new IllegalArgumentException("now, lastProlong and validUntil must not be null");
        }

        long daysRemaining = Math.max(0, ChronoUnit.DAYS.between(now, validUntil));
        long totalDays = ChronoUnit.DAYS.between(lastProlong, validUntil);

        int percent = totalDays > 0
                ? (int) Math.round(100.0 * daysRemaining / totalDays)
                : 0;

        return new PassProgress(daysRemaining, percent);
    }

    public static final class PassProgress {
        private final long daysRemaining;
        private final int percent;

        public PassProgress(long daysRemaining, int percent) {
            this.daysRemaining = daysRemaining;
            this.percent = percent;
        }

        public long getDaysRemaining() {
            return daysRemaining;
        }

        public int getPercent() {
            return percent;
        }
    }
}