package org.joupen.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class LoginAddAndRemovePlayerCommand extends AbstractCommand {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final TransactionManager transactionManager;

    public LoginAddAndRemovePlayerCommand(PlayerRepository playerRepository, TransactionManager transactionManager) {
        super("joupen");
        this.playerRepository = playerRepository;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
        this.transactionManager = transactionManager;
    }

    @Override
    public void execute(CommandSender commandSender, Command command, String string, String[] args) {
        log.info("Executing command /joupen by {} with args: {}", commandSender.getName(), Arrays.toString(args));
        if (args.length < 1) {
            commandSender.sendMessage(Component.text("Wrong amount of arguments. Try /joupen help", NamedTextColor.GOLD));
            log.warn("Command /joupen failed: insufficient arguments");
            return;
        }

        switch (args[0]) {
            case "help" -> helpCommand(commandSender);
            case "info" -> infoCommand(commandSender, args);
            case "prolong" -> prolongCommand(commandSender, args, false);
            case "gift" -> prolongCommand(commandSender, args, true);
            case "link" -> linkCommand(commandSender);
            case "addAllToWhitelist" -> addCommand(commandSender, args);
            default -> {
                commandSender.sendMessage(Component.text("Unknown subcommand. Try /joupen help", NamedTextColor.RED));
                log.warn("Unknown subcommand: {}", args[0]);
            }
        }
    }

    private boolean checkPermission(CommandSender commandSender, String permission) {
        if (!commandSender.hasPermission(permission)) {
            commandSender.sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
            log.warn("Permission {} denied for {}", permission, commandSender.getName());
            return true;
        }
        log.info("Permission {} granted for {}", permission, commandSender.getName());
        return false;
    }

    private void helpCommand(CommandSender commandSender) {
        TextComponent textComponent = Component.text()
                .append(Component.text("Joupen", NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Вайтлист плагин для ДжоуТека", NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Help:", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("/joupen help", NamedTextColor.GREEN))
                .append(Component.text(" - показывает эту страницу", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("/joupen prolong <player> [amount] [d/m]", NamedTextColor.GREEN))
                .append(Component.text(" - добавляет игрока на какое-то время. Default: 1 month", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("/joupen gift <player> [amount] [d/m]", NamedTextColor.GREEN))
                .append(Component.text(" - добавляет бесплатного игрока на какое-то время. Default: 1 month", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("/joupen info {for OP: player}", NamedTextColor.GREEN))
                .append(Component.text(" - показывает информацию о вашей проходке. Админ может смотреть всех игроков", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("Developed by ", NamedTextColor.GRAY))
                .append(Component.text("Lapitaniy ", NamedTextColor.DARK_AQUA))
                .append(Component.text("The ", NamedTextColor.RED))
                .append(Component.text("Гр", NamedTextColor.WHITE))
                .append(Component.text("ыб", NamedTextColor.RED))
                .append(Component.text("ни", NamedTextColor.WHITE))
                .append(Component.text("к", NamedTextColor.RED))
                .build();
        commandSender.sendMessage(textComponent);
        log.info("Displayed help for {}", commandSender.getName());
    }

    private void infoCommand(CommandSender commandSender, String[] args) {
        log.info("Executing /joupen info with args: {} by {}", Arrays.toString(args), commandSender);
        String playerName = (args.length > 1 && !checkPermission(commandSender, "joupen.admin")) ? args[1] : commandSender.getName();

        Optional<PlayerEntity> optionalEntity = playerRepository.findByName(playerName);
        if (optionalEntity.isEmpty()) {
            if (args.length > 1) {
                commandSender.sendMessage(Component.text("Can't find player with name " + playerName, NamedTextColor.RED));
                log.warn("Player {} not found for info command", playerName);
            } else {
                commandSender.sendMessage(Component.text("SOMETHING WENT WRONG! JOUPEN PLUGIN COULDN'T FIND INFO ABOUT YOU! PLEASE CONTACT ENDERDISSA", NamedTextColor.RED));
                log.error("Can't find info about existing player {}!", playerName);
            }
            return;
        }

        PlayerDto playerDto = playerMapper.entityToDto(optionalEntity.get());
        TextComponent textComponent = Component.text()
                .append(Component.text(args.length > 1 ? "Ник Игрока:" : "Твой Ник:", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getName(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text(args.length > 1 ? "UUID Игрока:" : "Твой UUID:", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getUuid().toString(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("Последняя дата продления проходки: ", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getLastProlongDate().toString(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("Проходка активна до: ", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getValidUntil().toString(), NamedTextColor.BLUE))
                .build();

        commandSender.sendMessage(textComponent);
        log.info("Displayed info for player {} to {}", playerName, commandSender.getName());
    }

    private Duration parseDuration(String durationStr) {
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

    private String formatDuration(Duration duration) {
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

    private void prolongCommand(CommandSender commandSender, String[] args, boolean gift) {
        log.info("Executing /joupen prolong with args: {}, gift: {}", Arrays.toString(args), gift);
        if (checkPermission(commandSender, "joupen.admin")) {
            log.warn("Prolong command aborted: {} lacks joupen.admin permission", commandSender.getName());
            return;
        }

        if (args.length < 2) {
            commandSender.sendMessage(Component.text("Wrong amount of arguments. Try /joupen help", NamedTextColor.RED));
            log.warn("Prolong command failed: insufficient arguments");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration;
        if (args.length >= 3) {
            try {
                duration = parseDuration(args[2]);
            } catch (Exception e) {
                commandSender.sendMessage(Component.text("Invalid duration format. Use format like 3d, 2h, or 7m (e.g., 3d2h7m)", NamedTextColor.RED));
                log.error("Failed to parse duration '{}': {}", args[2], e.getMessage());
                return;
            }
        } else {
            duration = Duration.ofDays(30); // Default: 1 month (30 days)
            log.info("Using default duration: 30 days");
        }

        if ("all".equals(args[1])) {
            log.info("Processing prolongation for all players");
            List<PlayerEntity> players = playerRepository.findAll();
            players.forEach(entity -> {
                PlayerDto playerDto = playerMapper.entityToDto(entity);
                if (!playerDto.getPaid() && !gift) {
                    log.info("Skipping player {}: not paid and not a gift", playerDto.getName());
                    return;
                }
                LocalDateTime validUntil = playerDto.getValidUntil().isBefore(now) ? now : playerDto.getValidUntil();
                playerDto.setValidUntil(validUntil.plus(duration));
                try {
                    playerRepository.updateByName(playerDto,playerDto.getName());
                    log.info("Updated player {}: new validUntil = {}", playerDto.getName(), playerDto.getValidUntil());
                } catch (Exception e) {
                    log.error("Failed to update player {} in all prolongation: {}", playerDto.getName(), e.getMessage());
                }
            });
            commandSender.sendMessage(Component.text("Gave everyone " + formatDuration(duration) + " time", NamedTextColor.RED));
            log.info("Prolongation for all players completed: {}", formatDuration(duration));
        } else {
            log.info("Processing prolongation for player: {}", args[1]);
            Optional<PlayerEntity> optionalEntity = playerRepository.findByName(args[1]);
            PlayerDto playerDto = optionalEntity.map(playerMapper::entityToDto).orElse(null);
            boolean isNew = playerDto == null;

            if (isNew) {
                playerDto = PlayerDto.builder()
                        .name(args[1])
                        .paid(!gift)
                        .lastProlongDate(now.minusDays(1))
                        .validUntil(now.minusDays(1))
                        .uuid(INITIAL_UUID.getUuid())
                        .build();
                log.info("Created new player DTO: {}", playerDto.getName());
            }

            LocalDateTime validUntil = playerDto.getValidUntil();
            if (validUntil.isBefore(now)) {
                validUntil = now;
                log.info("Updated lastProlongDate for {} to {}", playerDto.getName(), now);
            }
            playerDto.setValidUntil(validUntil.plus(duration));
            playerDto.setLastProlongDate(now);
            log.info("Set new validUntil for {} to {}", playerDto.getName(), playerDto.getValidUntil());

            if (isNew) {
                try {
                    playerRepository.save(playerDto);
                    TextComponent textComponent = Component.text()
                            .append(Component.text("Новый игрок ", NamedTextColor.AQUA))
                            .append(Component.text(args[1], NamedTextColor.YELLOW))
                            .append(Component.text(" впервые оплатил проходку! Ура!", NamedTextColor.AQUA))
                            .build();
                    Bukkit.getServer().sendMessage(textComponent);
                    commandSender.sendMessage(Component.text("Added new player to the whitelist: " + args[1], NamedTextColor.RED));
                    log.info("Saved new player to whitelist: {}", args[1]);
                } catch (Exception e) {
                    log.error("Failed to save new player {}: {}", args[1], e.getMessage());
                }
            } else {
                try {
                    playerRepository.updateByName(playerDto,playerDto.getName());
                    TextComponent textComponent = Component.text()
                            .append(Component.text("Игрок ", NamedTextColor.AQUA))
                            .append(Component.text(args[1], NamedTextColor.YELLOW))
                            .append(Component.text(" продлил проходку на еще ", NamedTextColor.AQUA))
                            .append(Component.text(formatDuration(duration), NamedTextColor.AQUA))
                            .append(Component.text(". Ура!", NamedTextColor.AQUA))
                            .build();
                    Bukkit.getServer().sendMessage(textComponent);
                    commandSender.sendMessage(Component.text("Added player to the whitelist: " + args[1], NamedTextColor.RED));
                    log.info("Player has renewed his subscription: {}", args[1]);
                } catch (Exception e) {
                    log.error("Failed to update player {}: {}", args[1], e.getMessage());
                }
            }
        }
    }

    private void linkCommand(CommandSender commandSender) {
        TextComponent textComponent = Component.text()
                .append(Component.text("Joupen", NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Ссылка на оплату проходочки ДжоуТека:", NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("https://clck.ru/3EEMC9", NamedTextColor.BLUE))
                .append(Component.text("*КЛИК*", NamedTextColor.GOLD))
                .clickEvent(ClickEvent.openUrl("https://forms.yandex.ru/u/6515e3dcd04688fca3cc271b/"))
                .build();
        commandSender.sendMessage(textComponent);
        log.info("Displayed payment link to {}", commandSender.getName());
    }

    private void addCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /joupen addAll <filePath> <days>", NamedTextColor.RED));
            return;
        }

        String filePath = args[1];
        int days;
        try {
            days = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Некорректное значение дней: " + args[2], NamedTextColor.RED));
            return;
        }

        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            path = Path.of("plugins", "joupen").resolve(filePath);
        }

        if (!Files.exists(path)) {
            sender.sendMessage(Component.text("Файл не найден: " + path, NamedTextColor.RED));
            return;
        }

        try {
            List<String> names = Files.readAllLines(path);
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.ofDays(days);

            int imported = 0;
            for (String name : names) {
                if (name.isBlank()) continue;

                Optional<PlayerEntity> optionalEntity = playerRepository.findByName(name.trim());
                if (optionalEntity.isPresent()) {
                    log.info("Player {} already in DB, skip", name);
                    continue;
                }

                PlayerDto playerDto = PlayerDto.builder()
                        .name(name.trim())
                        .paid(true)
                        .lastProlongDate(now)
                        .validUntil(now.plus(duration))
                        .uuid(INITIAL_UUID.getUuid())
                        .build();

                playerRepository.save(playerDto);
                imported++;
                log.info("Added new player: {} for {} day(days)", name, days);
            }

            sender.sendMessage(Component.text("Импортировано " + imported + " игроков из файла " + path, NamedTextColor.GREEN));
        } catch (IOException e) {
            sender.sendMessage(Component.text("Ошибка при чтении файла: " + e.getMessage(), NamedTextColor.RED));
            log.error("Ошибка чтения файла {}: {}", filePath, e.getMessage());
        } catch (Exception e) {
            sender.sendMessage(Component.text("Неожиданная ошибка: " + e.getMessage(), NamedTextColor.RED));
            log.error("Неожиданная ошибка: {}", e.getMessage());
        }
    }

}