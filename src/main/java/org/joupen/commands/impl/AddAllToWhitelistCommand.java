package org.joupen.commands.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;
import org.joupen.domain.PlayerEntity;
import org.joupen.messaging.Messaging;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;

import java.nio.file.Path;
import java.util.List;

@CommandAlias(
        name = "addalltowhitelist",
        minArgs = 2,
        maxArgs = 2,
        usage = "/joupen addAllToWhitelist <filePath> <days>",
        permission = "joupen.admin"
)
public class AddAllToWhitelistCommand implements GameCommand {
    private final CommandSender sender;
    private final PlayerImportService importService;
    private final PlayerService playerService;
    private final String filePathRaw;
    private final String daysRaw;

    public AddAllToWhitelistCommand(BuildContext buildContext) {
        this.sender = buildContext.getSender();
        this.importService = new PlayerImportService(buildContext.getPlayerRepository());
        this.playerService = buildContext.getPlayerService();
        String[] args = buildContext.getArgs();
        this.filePathRaw = args[0];
        this.daysRaw = args[1];
    }

    @Override
    public void execute() {
        int days = Integer.parseInt(daysRaw);

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
