package org.joupen;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.joupen.commands.impl.JoupenCommand;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.events.PlayerJoinEventHandler;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.events.StartupLoginGuard;
import org.joupen.events.listeners.PlayerProlongedBroadcastListener;
import org.joupen.messaging.Messaging;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.PlayerRepositoryFactory;
import org.joupen.service.PlayerService;
import org.joupen.service.ScheduledGiftService;
import org.joupen.utils.EventUtils;
import org.joupen.utils.JoupenProperties;

import java.nio.file.Path;

@Getter
@Slf4j
public class JoupenPlugin extends JavaPlugin {
    @Getter
    private static JoupenPlugin instance;
    private PlayerRepository playerRepository;
    private DatabaseManager databaseManager;
    private TransactionManager transactionManager;
    private ScheduledGiftService scheduledGiftService;
    private PlayerService playerService;
    private StartupLoginGuard startupLoginGuard;

    @Override
    public void onEnable() {
        instance = this;
        startupLoginGuard = new StartupLoginGuard();
        Bukkit.getPluginManager().registerEvents(startupLoginGuard, this);

        try {
            JoupenProperties.initialize(this.getDataFolder());
        } catch (Exception e) {
            failClosed("Failed to initialize JoupenProperties", e);
            return;
        }

        if (!JoupenProperties.enabled) {
            disable("Plugin disabled in config.yml");
            return;
        }

        try {
            if (JoupenProperties.useSql) {
                databaseManager = new DatabaseManager();
                transactionManager = new TransactionManager(databaseManager);
            }
            this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(transactionManager);

            log.info("Using profile with repository {}", playerRepository.getClass().getSimpleName());
        } catch (Exception e) {
            failClosed("Failed to initialize repository", e);
            return;
        }

        try {
            Messaging.initialize();
            org.joupen.utils.ReflectionUtils.init();

            new JoupenCommand(playerRepository, transactionManager);
            Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository), this);

            EventUtils.register(PlayerProlongedEvent.class, new PlayerProlongedBroadcastListener());
            playerService = new PlayerService(playerRepository);
            Path scheduledGiftsPath = this.getDataFolder().toPath().resolve("scheduled-gifts.txt");
            scheduledGiftService = new ScheduledGiftService(this, playerService, scheduledGiftsPath);
            scheduledGiftService.start();
            startupLoginGuard.markReady();
        } catch (Exception e) {
            failClosed("Failed to initialize services", e);
            return;
        }

        log.info("JoupenPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        log.info("JoupenPlugin disabling...");
        if (scheduledGiftService != null) {
            scheduledGiftService.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        log.info("JoupenPlugin disabled!");
    }

    private void disable(String reason) {
        log.error(reason);
        getServer().getPluginManager().disablePlugin(this);
    }

    private void failClosed(String reason, Exception exception) {
        log.error("{}. All logins are blocked.", reason, exception);
    }
}
