package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;
import org.joupen.utils.TimeUtils;

@CommandAlias(name = "adjust", minArgs = 2, maxArgs = Integer.MAX_VALUE,
        usage = "/joupen adjust <player> <+/-duration> [reason]", permission = "joupen.admin")
public class AdjustCommand extends OperationCommand {
    public AdjustCommand(BuildContext context) {
        super(context);
    }

    @Override
    protected void run(OperationArguments args) {
        replyResult(playerService.getOperations().adjust(args.get(0), TimeUtils.parseAdjustment(args.get(1)), args.metadata(sender, 2)));
    }
}
