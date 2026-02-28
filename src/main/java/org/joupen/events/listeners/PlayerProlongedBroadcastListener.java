package org.joupen.events.listeners;

import net.kyori.adventure.text.Component;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.messaging.Messaging;
import org.joupen.utils.TimeUtils;

import java.util.function.Consumer;

public class PlayerProlongedBroadcastListener implements Consumer<PlayerProlongedEvent> {

    @Override
    public void accept(PlayerProlongedEvent event) {
        String durationText = TimeUtils.formatDuration(event.duration());
        String msg = event.gift()
                ? "Игрок " + event.player().getName() + " получил подарок на " + durationText + "!"
                : "Игрок " + event.player().getName() + " продлил проходку на " + durationText + "!";
        Messaging.broadcast(Component.text(msg));
    }
}
