package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;

@CommandAlias(name = "undo", minArgs = 1, maxArgs = Integer.MAX_VALUE,
        usage = "/joupen undo <player> [last|id] [reason]", permission = "joupen.admin")
public class UndoCommand extends OperationCommand {
    public UndoCommand(BuildContext context) {
        super(context);
    }

    @Override
    protected void run(OperationArguments args) {
        Long id = args.values().size() < 2 || "last".equalsIgnoreCase(args.get(1)) ? null : Long.parseLong(args.get(1));
        replyResult(playerService.getOperations().undo(args.get(0), id, args.metadata(sender, 2)));
    }
}
