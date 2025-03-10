package org.joutak.loginpluginforjoutak.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.inputoutput.JsonWriterImpl;
import org.joutak.loginpluginforjoutak.inputoutput.Reader;
import org.joutak.loginpluginforjoutak.inputoutput.Writer;
import org.joutak.loginpluginforjoutak.repository.PlayerDtosUtils;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

import java.time.LocalDate;
import java.util.List;

import static org.joutak.loginpluginforjoutak.enums.UUIDTypes.INITIAL_UUID;

@Slf4j
public class LoginAddAndRemovePlayerCommand extends AbstractCommand {

    public LoginAddAndRemovePlayerCommand() {
        super("joupen");
    }

    @Override
    public void execute(CommandSender commandSender, Command command, String string, String[] args) {

        if (args.length < 1) {
            TextComponent textComponent = Component.text("Wrong amount of arguments. Try /joupen help", NamedTextColor.GOLD)
                    .toBuilder().build();
            commandSender.sendMessage(textComponent);
        }

        if (args[0].equals("help")) {
            helpCommand(commandSender);
        }

        if (args[0].equals("info")) {
            infoCommand(commandSender, args);
        }

        if (args[0].equals("prolong")) {
            prolongCommand(commandSender, args, false);
        }

        if (args[0].equals("gift")) {
            prolongCommand(commandSender, args, true);
        }

        if (args[0].equals("link")) {
            linkCommand(commandSender);
        }

    }

    private boolean checkPermission(CommandSender commandSender, String permission) {
        if (!commandSender.hasPermission(permission)) {
            TextComponent textComponent = Component.text("Go walk around. You don't have permission", NamedTextColor.RED)
                    .toBuilder().build();
            commandSender.sendMessage(textComponent);
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
        if (args.length > 1) {
            if (checkPermission(commandSender, "joupen.admin")) {
                return;
            }
            PlayerDto playerDto = PlayerDtosUtils.findPlayerByName(args[1]);

            if (playerDto == null) {
                TextComponent textComponent = Component.text("Can't find player with name " + args[1], NamedTextColor.RED);
                commandSender.sendMessage(textComponent);
                return;
            }

            TextComponent textComponent = Component.text()
                    .append(Component.text("Ник Игрока:", NamedTextColor.GREEN))
                    .append(Component.text(playerDto.getName(), NamedTextColor.BLUE))
                    .appendNewline()
                    .append(Component.text("UUID Игрока:", NamedTextColor.GREEN))
                    .append(Component.text(playerDto.getUuid().toString(), NamedTextColor.BLUE))
                    .appendNewline()
                    .append(Component.text("Последняя дата продления проходки: ", NamedTextColor.GREEN))
                    .append(Component.text(playerDto.getLastProlongDate().toString(), NamedTextColor.BLUE))
                    .appendNewline()
                    .append(Component.text("Проходка активна до: ", NamedTextColor.GREEN))
                    .append(Component.text(playerDto.getValidUntil().toString(), NamedTextColor.BLUE))
                    .build();

            commandSender.sendMessage(textComponent);
            return;
        }

        PlayerDto playerDto = PlayerDtosUtils.findPlayerByName(commandSender.getName());

        if (playerDto == null) {
            TextComponent textComponent = Component.text("SOMETHING WENT WRONG! " +
                    "JOUPEN PLUGIN COULDN'T FIND INFO ABOUT YOU!" +
                    " PLEASE CONTACT THE ADMINISTRATOR (ENDERDISSA)", NamedTextColor.RED);
            commandSender.sendMessage(textComponent);
            log.error("CAN'T FIND INFO ABOUT EXISTING PLAYER {} !!!! CHECK IT CAREFULLY!!!!", commandSender.getName());
            return;
        }

        TextComponent textComponent = Component.text()
                .append(Component.text("Твой Ник:", NamedTextColor.GREEN))
                .append(Component.text(playerDto.getName(), NamedTextColor.BLUE))
                .appendNewline()
                .append(Component.text("Твой UUID:", NamedTextColor.GREEN))
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
        // check perms
        if (checkPermission(commandSender, "joupen.admin")) {
            return;
        }

        // check args
        if (args.length < 2) {
            TextComponent textComponent = Component.text("Wrong amount of arguments." +
                    " Try /joupen help", NamedTextColor.RED);

            commandSender.sendMessage(textComponent);
            return;
        }

        // init vars
        Reader reader = new JsonReaderImpl(JoutakProperties.saveFilepath);
        Writer writer = new JsonWriterImpl(JoutakProperties.saveFilepath);
        LocalDate now = LocalDate.now();

        // parse period
        int daysAmount;
        if (args.length >= 3) {
            if (args.length >= 4) {
                if (args[3].equals("d")) {
                    daysAmount = Integer.parseInt(args[2]);
                } else {
                    daysAmount = 30 * Integer.parseInt(args[2]);
                }
            } else {
                daysAmount = 30 * Integer.parseInt(args[2]);
            }
        } else {
            daysAmount = 30;
        }

        // if all, gift everyone
        if (args[1].equals("all")) {
            PlayerDtos playerDtos = reader.read();
            playerDtos.getPlayerDtoList().forEach(
                    player -> {
                        if (!player.isPaid() && !gift) {
                            return;
                        }
                        LocalDate validUntil = player.getValidUntil();
                        if (validUntil.isBefore(now)) {
                            validUntil = now;
                        }
                        validUntil = validUntil.plusDays(daysAmount);
                        player.setValidUntil(validUntil);
                    }
            );
            writer.write(playerDtos);
            TextComponent textComponent = Component.text("Gave everyone " + daysAmount + " days", NamedTextColor.RED);
            commandSender.sendMessage(textComponent);
            return;
        }

        // get player
        PlayerDto playerDto = PlayerDtosUtils.findPlayerByName(args[1]);

        // init if new
        boolean isNew = false;
        if (playerDto == null) {
            isNew = true;
            playerDto = new PlayerDto();
            playerDto.setName(args[1]);
            playerDto.setPaid(!gift);
            playerDto.setLastProlongDate(now.minusDays(1));
            playerDto.setValidUntil(now.minusDays(1));
            playerDto.setUuid(INITIAL_UUID.getUuid());
        }

        LocalDate validUntil = playerDto.getValidUntil();

        // if pay streak broken
        if (validUntil.isBefore(LocalDate.now())) {
            playerDto.setLastProlongDate(now);
            validUntil = now;
        }

        validUntil = validUntil.plusDays(daysAmount);
        playerDto.setValidUntil(validUntil);

        PlayerDtos playerDtos = reader.read();
        PlayerDto currPlayerDto = PlayerDtosUtils.findPlayerByName(playerDto.getName());
        List<PlayerDto> list = playerDtos.getPlayerDtoList();
        int index = list.indexOf(currPlayerDto);
        list.set(index, playerDto);

        writer.write(playerDtos);
        if (!isNew) {
            TextComponent textComponent = Component.text()
                    .append(Component.text("Игрок ", NamedTextColor.AQUA))
                    .append(Component.text(args[1], NamedTextColor.YELLOW))
                    .append(Component.text(" продлил проходку на еще ", NamedTextColor.AQUA))
                    .append(Component.text(daysAmount, NamedTextColor.YELLOW))
                    .append(Component.text(" дней. Ура!", NamedTextColor.AQUA))
                    .build();
            Bukkit.broadcast(textComponent);
            textComponent = Component.text("Added player to the whitelist: " + args[1], NamedTextColor.RED);
            commandSender.sendMessage(textComponent);
            log.warn("Added player to the whitelist: {}", args[1]);
        } else {
            TextComponent textComponent = Component.text()
                    .append(Component.text("Новый игрок ", NamedTextColor.AQUA))
                    .append(Component.text(args[1], NamedTextColor.YELLOW))
                    .append(Component.text(" впервые оплатил проходку! Ура!", NamedTextColor.AQUA))
                    .build();
            Bukkit.broadcast(textComponent);
            textComponent = Component.text("Added new player to the whitelist: " + args[1], NamedTextColor.RED);
            commandSender.sendMessage(textComponent);
            log.warn("Added new player to the whitelist: {}", args[1]);
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
