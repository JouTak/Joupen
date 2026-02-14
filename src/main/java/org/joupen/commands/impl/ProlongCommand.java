package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.joupen.commands.BuildContext;
import org.joupen.validation.CommandValidator;
import org.joupen.commands.GameCommand;
import org.joupen.service.PlayerService;
import org.joupen.utils.TimeUtils;
import org.joupen.validation.Validator;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
public class ProlongCommand implements GameCommand, CommandValidator {
    private final PlayerService playerService;
    private final String target;
    private final String durationRaw;
    private final Duration defaultDuration;
    private final boolean gift;

    @Override
    public void execute() {
        Duration duration = durationRaw == null || durationRaw.isBlank() ? defaultDuration : TimeUtils.parseDuration(durationRaw);
        if ("all".equalsIgnoreCase(target)) {
            playerService.prolongAll(duration, gift);
        } else {
            playerService.prolongOne(target, duration, gift);
        }
    }

    @Override
    public List<Component> validate(BuildContext ctx, String[] args) {
        return Validator.of(ctx, args)
                .permission("joupen.admin")
                .usage("/joupen prolong <player|all> [duration]")
                .arg(0, "player name or 'all'")
                .optionalDuration(1, "3d2h7m")
                .check();
    }
}
