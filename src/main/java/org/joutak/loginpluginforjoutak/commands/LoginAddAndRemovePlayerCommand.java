package org.joutak.loginpluginforjoutak.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.mapper.PlayerMapper;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.joutak.loginpluginforjoutak.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class LoginAddAndRemovePlayerCommand extends AbstractCommand {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    public LoginAddAndRemovePlayerCommand(PlayerRepository playerRepository) {
        super("joupen");
        this.playerRepository = playerRepository;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public void execute(CommandSender commandSender, Command command, String string, String[] args) {
        if (args.length < 1) {
            TextComponent textComponent = Component.text("Wrong amount of arguments. Try /joupen help", NamedTextColor.GOLD)
                    .toBuilder().build();
            commandSender.sendMessage(textComponent);
            return;
        }

        switch (args[0]) {
            case "help" -> helpCommand(commandSender);
            case "info" -> infoCommand(commandSender, args);
            case "prolong" -> prolongCommand(commandSender, args, false);
            case "gift" -> prolongCommand(commandSender, args, true);
            case "link" -> linkCommand(commandSender);
        }
    }

    private boolean checkPermission(CommandSender commandSender, String permission) {
        if (!commandSender.hasPermission(permission)) {
            commandSender.sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
            return true;
        }
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
    }

    private void infoCommand(CommandSender commandSender, String[] args) {
        String playerName = (args.length > 1 && !checkPermission(commandSender, "joupen.admin")) ? args[1] : commandSender.getName();

        Optional<PlayerEntity> optionalEntity = playerRepository.findByName(playerName);
        if (optionalEntity.isEmpty()) {
            if (args.length > 1) {
                commandSender.sendMessage(Component.text("Can't find player with name " + playerName, NamedTextColor.RED));
            } else {
                commandSender.sendMessage(Component.text("SOMETHING WENT WRONG! JOUPEN PLUGIN COULDN'T FIND INFO ABOUT YOU! PLEASE CONTACT ENDERDISSA", NamedTextColor.RED));
                log.error("CAN'T FIND INFO ABOUT EXISTING PLAYER {}!", playerName);
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
    }

    private void prolongCommand(CommandSender commandSender, String[] args, boolean gift) {
        if (checkPermission(commandSender, "joupen.admin")) return;

        if (args.length < 2) {
            commandSender.sendMessage(Component.text("Wrong amount of arguments. Try /joupen help", NamedTextColor.RED));
            return;
        }

        LocalDate now = LocalDate.now();
        int daysAmount = args.length >= 3
                ? (args.length >= 4 && "d".equals(args[3]) ? Integer.parseInt(args[2]) : 30 * Integer.parseInt(args[2]))
                : 30;

        if ("all".equals(args[1])) {
            List<PlayerEntity> players = playerRepository.findAll();
            players.forEach(entity -> {
                PlayerDto playerDto = playerMapper.entityToDto(entity);
                if (!playerDto.isPaid() && !gift) return;
                LocalDate validUntil = playerDto.getValidUntil().isBefore(now) ? now : playerDto.getValidUntil();
                playerDto.setValidUntil(validUntil.plusDays(daysAmount));
                try {
                    playerRepository.update(playerDto);
                } catch (Exception e) {
                    log.error("Failed to update player {} in all prolongation: {}", playerDto.getName(), e.getMessage());
                }
            });
            commandSender.sendMessage(Component.text("Gave everyone " + daysAmount + " days", NamedTextColor.RED));
            return;
        }

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
        }

        LocalDate validUntil = playerDto.getValidUntil();
        if (validUntil.isBefore(now)) {
            playerDto.setLastProlongDate(now);
            validUntil = now;
        }
        playerDto.setValidUntil(validUntil.plusDays(daysAmount));

        try {
            if (isNew) {
                playerRepository.save(playerDto);
                Bukkit.broadcast(Component.text("Новый игрок " + args[1] + " впервые оплатил проходку! Ура!", NamedTextColor.AQUA));
                commandSender.sendMessage(Component.text("Added new player to the whitelist: " + args[1], NamedTextColor.RED));
                log.warn("Added new player to the whitelist: {}", args[1]);
            } else {
                playerRepository.update(playerDto);
                Bukkit.broadcast(Component.text("Игрок " + args[1] + " продлил проходку на еще " + daysAmount + " дней. Ура!", NamedTextColor.AQUA));
                commandSender.sendMessage(Component.text("Added player to the whitelist: " + args[1], NamedTextColor.RED));
                log.warn("Added player to the whitelist: {}", args[1]);
            }
        } catch (Exception e) {
            log.error("Failed to save/update player {}: {}", args[1], e.getMessage());
            commandSender.sendMessage(Component.text("Error saving player data. Contact administrator.", NamedTextColor.RED));
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
    }
}
