package org.joupen.commands.impl;

import org.bukkit.command.CommandSender;
import org.joupen.service.PlayerService;

import java.time.Duration;

public class GiftCommand extends ProlongCommand {
    public GiftCommand(CommandSender sender, PlayerService service, String target, Duration duration) {
        super(service, target, duration, true);
    }
}
