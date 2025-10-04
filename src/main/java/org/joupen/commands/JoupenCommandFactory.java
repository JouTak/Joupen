package org.joupen.commands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joupen.commands.impl.*;
import org.joupen.events.SendPrivateMessageEvent;
import org.joupen.service.PlayerImportService;
import org.joupen.service.PlayerService;
import org.joupen.utils.EventUtils;
import org.joupen.utils.TimeUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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

        PlayerService ps = ctx.getPlayerService() != null ? ctx.getPlayerService() : new PlayerService(ctx.getPlayerRepository());

        GameCommand command = switch (sub) {
            case "help" -> new HelpCommand(ctx.getSender());
            case "link" -> new LinkCommand(ctx.getSender());
            case "info" ->
                    new InfoCommand(ctx.getSender(), ctx.getPlayerRepository(), ctx.getPlayerMapper(), tail.length == 0 ? ctx.getSender().getName() : tail[0]);
            case "prolong" ->
                    new ProlongCommand(ps, tail.length > 0 ? tail[0] : "", tail.length >= 2 ? TimeUtils.parseDuration(tail[1]) : Duration.ofDays(30), false);
            case "gift" ->
                    new GiftCommand(ctx.getSender(), ps, tail.length > 0 ? tail[0] : "", tail.length >= 2 ? TimeUtils.parseDuration(tail[1]) : Duration.ofDays(30));
            case "addalltowhitelist" ->
                    new AddAllToWhitelistCommand(ctx.getSender(), new PlayerImportService(ctx.getPlayerRepository()), ps, tail.length > 0 ? tail[0] : "", tail.length > 1 ? tail[1] : "");
            default ->
                    () -> EventUtils.publish(new SendPrivateMessageEvent(ctx.getSender(), Component.text("Unknown subcommand. Try /joupen help", NamedTextColor.RED)));
        };

        if (command instanceof CommandValidator validator) {
            List<Component> errors = validator.validate(ctx, tail);
            if (!errors.isEmpty()) {
                return () -> errors.forEach(msg -> EventUtils.publish(new SendPrivateMessageEvent(ctx.getSender(), msg)));
            }
        }
        return command;
    }
}