package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;

@CommandAlias(name="approve",minArgs=1,
        usage = "/joupen approve <player> [reason]", permission = "joupen.admin")
public class ApproveCommand extends OperationCommand {

    public ApproveCommand(BuildContext context) { super(context); }

    @Override
    protected void run(OperationArguments args) {
        replyResult(playerService.getOperations().approval(args.get(0),true,args.metadata(sender,1)));
    }
}
