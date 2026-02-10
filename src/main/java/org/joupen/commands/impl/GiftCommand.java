package org.joupen.commands.impl;

import org.joupen.service.PlayerService;

import java.time.Duration;

public class GiftCommand extends ProlongCommand {
    public GiftCommand(PlayerService service, String target, String durationRaw, Duration defaultDuration) {
        super(service, target, durationRaw, defaultDuration, true);
    }
}
