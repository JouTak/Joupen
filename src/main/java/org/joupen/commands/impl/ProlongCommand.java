package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;
import org.joupen.utils.TimeUtils;

import java.time.Duration;
import java.util.Map;

@CommandAlias(
        name = "prolong",
        minArgs = 1,
        maxArgs = Integer.MAX_VALUE,
        usage = "/joupen prolong <player|all> [duration] [reason]",
        permission = "joupen.admin"
)
public class ProlongCommand extends OperationCommand {
    private final boolean gift;

    public ProlongCommand(BuildContext context) {
        this(context, false);
    }

    protected ProlongCommand(BuildContext context, boolean gift) {
        super(context);
        this.gift = gift;
    }

    @Override
    protected void run(OperationArguments args) {
        String target = args.get(0);
        Duration duration = args.values().size() < 2 ? Duration.ofDays(30) : TimeUtils.parseDuration(args.get(1));
        var metadata = args.metadata(sender, 2);
        if ("all".equalsIgnoreCase(target)) {
            playerService.prolongAll(duration, gift, metadata);
            reply("batch-applied", Map.of());
        } else {
            replyResult(playerService.prolongOne(target, duration, gift, metadata));
        }
    }
}
