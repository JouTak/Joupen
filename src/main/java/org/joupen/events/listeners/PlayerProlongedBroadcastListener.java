package org.joupen.events.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.joupen.events.PlayerProlongedEvent;

import java.util.function.Consumer;

public class PlayerProlongedBroadcastListener implements Consumer<PlayerProlongedEvent> {

    @Override
    public void accept(PlayerProlongedEvent event) {
        try {
            String msg = event.gift() ? "Игрок " + event.player().getName() + " получил подарок!" : "Игрок " + event.player().getName() + " продлил проходку!";
            Bukkit.getServer().sendMessage(Component.text(msg));
        }catch (Exception e) {

        }
    }
}
