package org.joupen.commands.impl;

import lombok.RequiredArgsConstructor;
import org.joupen.commands.GameCommand;
import org.joupen.service.PlayerService;

import java.time.Duration;

@RequiredArgsConstructor
public class ProlongCommand implements GameCommand {
    private final PlayerService playerService;
    private final String target;
    private final Duration duration;
    private final boolean gift;

    @Override
    public void execute() {
        if ("all".equalsIgnoreCase(target)) {
            playerService.prolongAll(duration, gift);
        } else {
            playerService.prolongOne(target, duration, gift);
        }
    }
}
