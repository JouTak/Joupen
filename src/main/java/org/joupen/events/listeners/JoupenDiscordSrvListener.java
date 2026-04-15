package org.joupen.events.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joupen.bukkit.event.JoupenPassProlongedEvent;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.TimeUtils;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JoupenDiscordSrvListener implements Listener {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AtomicBoolean missingDiscordSrvWarned = new AtomicBoolean(false);
    private final AtomicBoolean missingChannelWarned = new AtomicBoolean(false);

    @EventHandler
    public void onPassProlonged(JoupenPassProlongedEvent event) {
        if (!JoupenProperties.discordSrvPassProlongedEnabled) {
            return;
        }

        DiscordSRV discordSRV = DiscordSRV.getPlugin();
        if (discordSRV == null || !discordSRV.isEnabled()) {
            warnOnce(missingDiscordSrvWarned, "DiscordSRV is not installed or disabled, pass prolongation notifications are skipped");
            return;
        }

        TextChannel channel = discordSRV.getDestinationTextChannelForGameChannelName(JoupenProperties.discordSrvPassProlongedChannel);
        if (channel == null) {
            warnOnce(
                    missingChannelWarned,
                    "DiscordSRV channel '{}' was not found, pass prolongation notifications are skipped",
                    JoupenProperties.discordSrvPassProlongedChannel
            );
            return;
        }

        String playerName = event.getName();
        String author = event.isGift()
                ? playerName + " получил проходку! (подарок)"
                : playerName + " получил проходку!";

        StringBuilder description = new StringBuilder("Дата окончания проходки: ")
                .append(event.getValidUntil().format(DATE_TIME_FORMATTER));

        if (JoupenProperties.discordSrvPassProlongedShowDuration) {
            description
                    .append("\nПродление: ")
                    .append(TimeUtils.formatDuration(event.getDuration()));
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(parseColor(JoupenProperties.discordSrvPassProlongedColor))
                .setAuthor(author, null, "https://crafthead.net/helm/" + event.getUuid())
                .setDescription(description.toString());

        channel.sendMessageEmbeds(embedBuilder.build()).queue(
                success -> {
                },
                error -> log.warn("Failed to send DiscordSRV pass prolongation notification: {}", error.getMessage())
        );
    }

    private Color parseColor(String hexColor) {
        try {
            return Color.decode(hexColor);
        } catch (NumberFormatException e) {
            log.warn("Invalid DiscordSRV embed color '{}', fallback to #30d5c8", hexColor);
            return Color.decode("#30d5c8");
        }
    }

    private void warnOnce(AtomicBoolean guard, String message, Object... args) {
        if (guard.compareAndSet(false, true)) {
            log.warn(message, args);
        }
    }
}
