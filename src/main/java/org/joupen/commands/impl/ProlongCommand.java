package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.GameCommand;
import org.joupen.service.PlayerService;
import org.joupen.utils.TimeUtils;

import java.time.Duration;

@CommandAlias(
        name = "prolong",
        minArgs = 1,
        maxArgs = 2,
        usage = "/joupen prolong <player|all> [duration]",
        permission = "joupen.admin"
)
public class ProlongCommand implements GameCommand {
    protected final PlayerService playerService;
    protected final String target;
    protected final String durationRaw;
    protected final Duration defaultDuration;
    protected final boolean gift;

    public ProlongCommand(BuildContext buildContext) {
        this(buildContext, false);
    }

    protected ProlongCommand(BuildContext buildContext, boolean gift) {
        this.playerService = buildContext.getPlayerService();
        String[] args = buildContext.getArgs();
        this.target = args.length > 0 ? args[0] : "";
        this.durationRaw = args.length >= 2 ? args[1] : "";
        this.defaultDuration = Duration.ofDays(30);
        this.gift = gift;
    }

    @Override
    public void execute() {
        Duration duration = durationRaw == null || durationRaw.isBlank()
                ? defaultDuration
                : TimeUtils.parseDuration(durationRaw);
        if ("all".equalsIgnoreCase(target)) {
            playerService.prolongAll(duration, gift);
        } else {
            playerService.prolongOne(target, duration, gift);
        }
    }
}
