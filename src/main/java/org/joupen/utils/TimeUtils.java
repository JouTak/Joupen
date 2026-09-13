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

    // --- твой существующий метод ---
    public static Duration parseDuration(String durationStr) {
        log.info("Parsing duration string: {}", durationStr);

        if (durationStr == null || durationStr.isBlank()) {
            throw new IllegalArgumentException("Duration string is blank");
        }

        Pattern pattern = Pattern.compile("(\\d+)(mo|[dhm])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(durationStr.toLowerCase());

        long days = 0;
        long hours = 0;
        long minutes = 0;
        long months = 0;
        boolean found = false;
        int end = 0;

        while (matcher.find()) {
            if (matcher.start() != end) throw new IllegalArgumentException("Invalid duration format: " + durationStr);
            end = matcher.end();
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "mo" -> months = Math.addExact(months, value);
                case "d" -> days = Math.addExact(days, value);
                case "h" -> hours = Math.addExact(hours, value);
                case "m" -> minutes = Math.addExact(minutes, value);
                default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
            }
        }

        if (!found || end != durationStr.length()) {
            log.warn("No valid duration found in string: {}", durationStr);
            throw new IllegalArgumentException("Invalid duration format: " + durationStr);
        }

        Duration duration = Duration.ofDays(Math.multiplyExact(months, 30L))
                .plusDays(days)
                .plusHours(hours)
                .plusMinutes(minutes);

        log.info("Parsed duration result: {} (months={}, days={}, hours={}, minutes={})",
                duration, months, days, hours, minutes);

        return duration;
    }

    public static Duration parseAdjustment(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Duration string is blank");
        boolean negative = value.startsWith("-");
        String duration = negative || value.startsWith("+") ? value.substring(1) : value;
        Duration parsed = parseDuration(duration);
        return negative ? parsed.negated() : parsed;
    }

    // --- твой существующий метод форматирования ---
    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "0m";
        }
        if (duration.isNegative()) return "-" + formatDuration(duration.negated());

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

    // --- новый метод: человекочитаемый формат с склонениями ---
    public static String formatDurationReadable(Duration duration) {
        if (duration == null) return "0 минут";

        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" ").append(pluralDays(days)).append(" ");
        if (hours > 0) sb.append(hours).append(" ").append(pluralHours(hours)).append(" ");
        if (minutes > 0) sb.append(minutes).append(" ").append(pluralMinutes(minutes)).append(" ");

        return sb.toString().trim();
    }

    private static String pluralDays(long n) {
        if (n % 10 == 1 && n % 100 != 11) return "день";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "дня";
        return "дней";
    }

    private static String pluralHours(long n) {
        if (n % 10 == 1 && n % 100 != 11) return "час";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "часа";
        return "часов";
    }

    private static String pluralMinutes(long n) {
        if (n % 10 == 1 && n % 100 != 11) return "минута";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "минуты";
        return "минут";
    }

    // --- твой существующий метод PassProgress ---
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
