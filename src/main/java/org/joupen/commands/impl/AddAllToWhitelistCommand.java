package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public class AddAllToWhitelistCommand implements GameCommand {
    private final CommandSender sender;
    private final PlayerImportService importService;
    private final PlayerService playerService;
    private final String filePathRaw;
    private final String daysRaw;

    @Override
    public void execute() {
        int days;
        try {
            days = Integer.parseInt(daysRaw);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Некорректное значение дней: " + daysRaw, NamedTextColor.RED));
            return;
        }

        Path path = Path.of(filePathRaw);
        if (!path.isAbsolute()) {
            path = Path.of("plugins", "joupen").resolve(filePathRaw);
        }

        try {
            List<PlayerEntity> imported = importService.buildNewPlayerFromFileWithNames(path, days);
            playerService.addAll(imported);

            sender.sendMessage(Component.text("Импортировано " + imported.size() + " игроков из " + path, NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Ошибка импорта: " + e.getMessage(), NamedTextColor.RED));
        }
    }
}
