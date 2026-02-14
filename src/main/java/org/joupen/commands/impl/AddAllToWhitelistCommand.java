package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.messaging.Messaging;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;
import org.joupen.validation.CommandValidator;
import org.joupen.validation.Validator;

import java.nio.file.Path;
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
        return Validator.of(ctx, args)
                .permission("joupen.admin")
                .usage("/joupen addAllToWhitelist <filePath> <days>")
                .arg(0, "file path")
                .intArg(1, "days")
                .check();
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

            Messaging.reply(sender, Component.text("Импортировано " + imported.size() + " игроков из " + path, NamedTextColor.GREEN));
        } catch (Exception e) {
            Messaging.reply(sender, Component.text("Ошибка импорта: " + e.getMessage(), NamedTextColor.RED));
        }
    }
}