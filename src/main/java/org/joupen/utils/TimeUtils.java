package org.joupen.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class TimeUtils {
    public static Duration parseDuration(String durationStr) {
        log.info("Parsing duration string: {}", durationStr);
        // Изменяем регулярное выражение, чтобы mo обрабатывалось до m
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
                    months = value;
                    log.info("Parsed months: {}", months);
                    break;
                case "d":
                    days = value;
                    log.info("Parsed days: {}", days);
                    break;
                case "h":
                    hours = value;
                    log.info("Parsed hours: {}", hours);
                    break;
                case "m":
                    minutes = value;
                    log.info("Parsed minutes: {}", minutes);
                    break;
            }
        }

        if (!found) {
            log.warn("No valid duration found in string: {}", durationStr);
            throw new IllegalArgumentException("Invalid duration format");
        }

        Duration duration = Duration.ofDays(months * 30L)
                .plusDays(days)
                .plusHours(hours)
                .plusMinutes(minutes);
        log.info("Parsed duration: {} months, {} days, {} hours, {} minutes", months, days, hours, minutes);
        return duration;
    }

    public static String formatDuration(Duration duration) {
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
}
