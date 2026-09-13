package org.joupen.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class OperationMessages {
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("operation-applied", "Операция #{id}: {type}, {duration}. Срок: {after}."),
            Map.entry("operation-duplicate", "Операция #{id} уже применена. Повтор пропущен."),
            Map.entry("undo-applied", "Операция #{reverses} отменена операцией #{id}. Изменение: {duration}. Срок: {after}."),
            Map.entry("batch-applied", "Продление выполнено. ID операций доступны через /joupen history <player>."),
            Map.entry("history-header", "История {player}, страница {page}:"),
            Map.entry("history-empty", "На этой странице нет операций."),
            Map.entry("history-entry", "#{id} | {date} | {type} {duration}\n{reason} | {source} | {initiator} | external={externalId} | undo={reverses}\nСрок: {before} → {after}; approval: {approvedBefore} → {approvedAfter}; temporary: {temporaryBefore} → {temporaryAfter}"),
            Map.entry("invalid-arguments", "Проверь аргументы команды: /joupen help."),
            Map.entry("invalid-duration", "Укажи ненулевое время: 1mo, 3d, 2h, 30m. Минус разрешён только для adjust."),
            Map.entry("invalid-metadata", "Проверь источник и внешний ID операции."),
            Map.entry("external-id-conflict", "Этот внешний ID уже использован для другого запроса."),
            Map.entry("operation-not-found", "Операция не найдена."),
            Map.entry("operation-not-reversible", "Эту операцию нельзя отменить. Используй adjust."),
            Map.entry("operation-already-reversed", "Эта операция уже отменена."),
            Map.entry("player-not-found", "Игрок не найден."),
            Map.entry("player-already-exists", "Игрок уже существует."),
            Map.entry("invalid-player", "Укажи ник игрока длиной до 16 символов."),
            Map.entry("pass-not-found", "У игрока нет срока проходки, из которого можно убрать время."),
            Map.entry("invalid-window", "Начало временного доступа должно быть раньше конца."),
            Map.entry("invalid-page", "Номер страницы должен быть положительным."),
            Map.entry("operation-failed", "Не удалось выполнить операцию. Подробности в логе сервера.")
    );
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");
    private static Map<String, String> messages = DEFAULTS;

    private OperationMessages() {
    }

    public static void configure(Map<String, Object> config) {
        Map<String, String> configured = new HashMap<>(DEFAULTS);
        DEFAULTS.keySet().forEach(key -> {
            if (config.get(key) instanceof String value) configured.put(key, value);
        });
        messages = Map.copyOf(configured);
    }

    public static String format(String key, Map<String, ?> values) {
        String template = messages.getOrDefault(key, messages.get("operation-failed"));
        return PLACEHOLDER.matcher(template).replaceAll(match -> Matcher.quoteReplacement(
                values.containsKey(match.group(1)) ? String.valueOf(values.get(match.group(1))) : match.group()));
    }

    public static String defaultsYaml() {
        return DEFAULTS.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> "  " + entry.getKey() + ": " + Utils.toJson(entry.getValue()) + "\n")
                .collect(Collectors.joining());
    }
}
