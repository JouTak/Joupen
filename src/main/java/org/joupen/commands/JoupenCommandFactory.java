package org.joupen.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.joupen.commands.impl.*;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;
import org.joupen.utils.TimeUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

@Slf4j
public class JoupenCommandFactory {

    public GameCommand build(BuildContext ctx) {
        String[] args = ctx.getArgsTail();
        log.info("Executing /{} by {} with args: {}", ctx.getLabel(), ctx.getSender().getName(), Arrays.toString(args));

        if (args.length == 0) {
            return new HelpCommand(ctx.getSender());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] tail = Arrays.copyOfRange(args, 1, args.length);

        return switch (sub) {
            case "help" -> new HelpCommand(ctx.getSender());
            case "link" -> new LinkCommand(ctx.getSender());
            case "info" -> buildInfo(ctx, tail);
            case "prolong" -> buildProlong(ctx, tail, false);
            case "gift" -> buildGift(ctx, tail);
            case "addalltowhitelist" -> buildAddAll(ctx, tail);
            default ->
                    () -> ctx.getSender().sendMessage(Component.text("Unknown subcommand. Try /joupen help", NamedTextColor.RED));
        };
    }

    private GameCommand buildInfo(BuildContext ctx, String[] tail) {
        if (tail.length == 0) {
            return new InfoCommand(ctx.getSender(), ctx.getPlayerRepository(), ctx.getPlayerMapper(), ctx.getSender().getName());
        }

        if (!hasPerm(ctx.getSender())) {
            return () -> ctx.getSender().sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }
        return new InfoCommand(ctx.getSender(), ctx.getPlayerRepository(), ctx.getPlayerMapper(), tail[0]);
    }

    private GameCommand buildProlong(BuildContext ctx, String[] tail, boolean gift) {

        if (!ctx.getSender().hasPermission("joupen.admin")) {
            return () -> ctx.getSender().sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }

        if (tail.length < 1) {
            return () -> ctx.getSender().sendMessage(Component.text("Usage: /joupen " + (gift ? "gift" : "prolong") + " <player|all> [duration]", NamedTextColor.RED));
        }

        String target = tail[0];
        Duration dur;
        try {
            dur = (tail.length >= 2) ? TimeUtils.parseDuration(tail[1]) : Duration.ofDays(30);
        } catch (Exception e) {
            return () -> ctx.getSender().sendMessage(Component.text("Invalid duration format. Example: 3d2h7m", NamedTextColor.RED));
        }

        PlayerService service = new PlayerService(ctx.getPlayerRepository());
        return gift ? new GiftCommand(ctx.getSender(), service, target, dur) : new ProlongCommand(service, target, dur, false);
    }

    private GameCommand buildGift(BuildContext ctx, String[] tail) {
        if (!hasPerm(ctx.getSender())) {
            return () -> ctx.getSender().sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }
        if (tail.length < 1) {
            return () -> ctx.getSender().sendMessage(Component.text("Usage: /joupen gift <player|all> [duration]", NamedTextColor.RED));
        }
        String target = tail[0];
        Duration dur = (tail.length >= 2) ? TimeUtils.parseDuration(tail[1]) : Duration.ofDays(30);

        PlayerService service = new PlayerService(ctx.getPlayerRepository());

        return new GiftCommand(ctx.getSender(), service, target, dur);
    }


    private GameCommand buildAddAll(BuildContext ctx, String[] tail) {
        if (!hasPerm(ctx.getSender())) {
            return () -> ctx.getSender().sendMessage(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }
        if (tail.length < 2) {
            return () -> ctx.getSender().sendMessage(Component.text("Usage: /joupen addAllToWhitelist <filePath> <days>", NamedTextColor.RED));
        }
        String file = tail[0];
        String days = tail[1];
        return new AddAllToWhitelistCommand(ctx.getSender(), new PlayerImportService(ctx.getPlayerRepository()), new PlayerService(ctx.getPlayerRepository()), file, days);
    }

    private boolean hasPerm(CommandSender sender) {
        return sender.hasPermission("joupen.admin");
    }
}
