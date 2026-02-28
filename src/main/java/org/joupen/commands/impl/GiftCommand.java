package org.joupen.commands.impl;

import org.joupen.commands.BuildContext;
import org.joupen.commands.CommandAlias;

@CommandAlias(
        name = "gift",
        minArgs = 1,
        maxArgs = 2,
        usage = "/joupen gift <player|all> [duration]",
        permission = "joupen.admin"
)
public class GiftCommand extends ProlongCommand {
    public GiftCommand(BuildContext buildContext) {
        super(buildContext, true);
    }
}
