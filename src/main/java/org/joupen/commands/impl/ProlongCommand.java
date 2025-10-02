package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandValidator;
import org.joupen.commands.GameCommand;
import org.joupen.service.PlayerService;
import org.joupen.utils.TimeUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ProlongCommand implements GameCommand, CommandValidator {
    private final PlayerService playerService;
    private final String target;
    private final Duration duration;
    private final boolean gift;

    @Override
    public void execute() {
        if ("all".equalsIgnoreCase(target)) {
            playerService.prolongAll(duration, gift);
        } else {
            playerService.prolongOne(target, duration, gift);
        }
    }

    @Override
    public List<Component> validate(BuildContext ctx, String[] args) {
        List<Component> errors = new ArrayList<>();
        if (!ctx.getSender().hasPermission("joupen.admin")) {
            errors.add(Component.text("Go walk around. You don't have permission", NamedTextColor.RED));
        }
        if (args.length < 1) {
            errors.add(Component.text("Usage: /joupen prolong <player|all> [duration]", NamedTextColor.RED));
        }
        if (args.length >= 2) {
            try {
                TimeUtils.parseDuration(args[1]);
            } catch (Exception e) {
                errors.add(Component.text("Invalid duration format. Example: 3d2h7m", NamedTextColor.RED));
            }
        }
        return errors;
    }
}
