package org.joupen.events.listeners;

import net.kyori.adventure.text.Component;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.messaging.Messaging;

import java.util.function.Consumer;

public class PlayerProlongedBroadcastListener implements Consumer<PlayerProlongedEvent> {

    @Override
    public void accept(PlayerProlongedEvent event) {
        String msg = event.gift()
                ? "Игрок " + event.player().getName() + " получил подарок!"
                : "Игрок " + event.player().getName() + " продлил проходку!";
        Messaging.broadcast(Component.text(msg));
    }
}
