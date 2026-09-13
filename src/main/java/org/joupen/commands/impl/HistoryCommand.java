package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;
import org.joupen.commands.OperationArguments;
import org.joupen.commands.OperationCommand;

import java.util.Map;

@CommandAlias(name = "history", minArgs = 1, maxArgs = 2,
        usage = "/joupen history <player> [page]", permission = "joupen.admin")
public class HistoryCommand extends OperationCommand {
    public HistoryCommand(BuildContext context) {
        super(context);
    }

    @Override
    protected void run(OperationArguments args) {
        int page = args.values().size() < 2 ? 1 : Integer.parseInt(args.get(1));
        var history = playerService.getOperations().history(args.get(0), page);
        reply("history-header", Map.of("player", args.get(0), "page", page));
        if (history.isEmpty()) reply("history-empty", Map.of());
        else history.forEach(operation -> replyOperation("history-entry", operation));
    }
}
