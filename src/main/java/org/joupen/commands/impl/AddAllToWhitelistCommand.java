package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandValidator;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.events.SendPrivateMessageEvent;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;
import org.joupen.utils.EventUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class AddAllToWhitelistCommand implements GameCommand, CommandValidator {
    private final CommandSender sender;
    private final PlayerImportService importService;
    private final PlayerService playerService;
    private final String filePathRaw;
    private final String daysRaw;

    @Override
    public List<Component> validate(BuildContext ctx, String[] args) {
        List<Component> errors = new ArrayList<>();
        if (!ctx.getSender().hasPermission("joupen.admin")) {
            errors.add(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }
        if (args.length < 2 || filePathRaw.isEmpty() || daysRaw.isEmpty()) {
            errors.add(Component.text("Usage: /joupen addAllToWhitelist <filePath> <days>", NamedTextColor.RED));
        } else {
            try {
                Integer.parseInt(daysRaw);
            } catch (NumberFormatException e) {
                errors.add(Component.text("Некорректное значение дней: " + daysRaw, NamedTextColor.RED));
            }
        }
        return errors;
    }

    @Override
    public void execute() {
        int days = Integer.parseInt(daysRaw); // Already validated

        Path path = Path.of(filePathRaw);
        if (!path.isAbsolute()) {
            path = Path.of("plugins", "joupen").resolve(filePathRaw);
        }

        try {
            List<PlayerEntity> imported = importService.buildNewPlayerFromFileWithNames(path, days);
            playerService.addAll(imported);

            EventUtils.publish(new SendPrivateMessageEvent(sender, Component.text("Импортировано " + imported.size() + " игроков из " + path, NamedTextColor.GREEN)));
        } catch (Exception e) {
            EventUtils.publish(new SendPrivateMessageEvent(sender, Component.text("Ошибка импорта: " + e.getMessage(), NamedTextColor.RED)));
        }
    }
}