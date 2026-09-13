package org.joupen.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.joupen.utils.JoupenProperties;

public class StartupLoginGuard implements Listener {
    private volatile boolean ready;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!ready) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    Component.text(JoupenProperties.accessCheckFailedMessage, NamedTextColor.RED));
        }
    }

    public void markReady() {
        ready = true;
    }
}
