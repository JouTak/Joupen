package org.joupen.events.listeners;

import net.kyori.adventure.text.Component;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.messaging.Messaging;

import java.util.function.Consumer;

public class PlayerProlongedBroadcastListener implements Consumer<PlayerProlongedEvent> {

    @Override
    public void accept(PlayerProlongedEvent event) {
        String msg = event.gift()
                ? "Игрок " + event.player().getName() + " получил подарок на" + event.duration() + "!"
                : "Игрок " + event.player().getName() + " продлил проходку" + event.duration() + "!";
        Messaging.broadcast(Component.text(msg));
    }
}
