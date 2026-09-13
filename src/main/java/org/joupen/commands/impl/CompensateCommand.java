package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;
import org.joupen.domain.OperationType;
import org.joupen.utils.TimeUtils;

@CommandAlias(name = "compensate", minArgs = 2, maxArgs = Integer.MAX_VALUE,
        usage = "/joupen compensate <player> <duration> [reason]", permission = "joupen.admin")
public class CompensateCommand extends OperationCommand {
    public CompensateCommand(BuildContext context) {
        super(context);
    }

    @Override
    protected void run(OperationArguments args) {
        replyResult(playerService.grant(args.get(0), TimeUtils.parseDuration(args.get(1)), OperationType.COMPENSATION, args.metadata(sender, 2)));
    }
}
