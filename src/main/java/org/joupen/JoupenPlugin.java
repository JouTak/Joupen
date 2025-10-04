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
import org.joupen.events.SendPrivateMessageEvent;
import org.joupen.events.listeners.PlayerProlongedBroadcastListener;
import org.joupen.events.listeners.PrivateMessageListener;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.PlayerRepositoryFactory;
import org.joupen.service.MigrationService;
import org.joupen.utils.EventUtils;
import org.joupen.utils.JoupenProperties;

@Getter
@Slf4j
public class JoupenPlugin extends JavaPlugin {
    @Getter
    private static JoupenPlugin instance;
    private PlayerRepository playerRepository;
    private DatabaseManager databaseManager;
    private TransactionManager transactionManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            JoupenProperties.initialize(this.getDataFolder());
        } catch (Exception e) {
            disable("Failed to initialize JoupenProperties: " + e.getMessage());
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
            this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(databaseManager, transactionManager);

            log.info("Using profile with repository {}", playerRepository.getClass().getSimpleName());
        } catch (Exception e) {
            disable("Failed to initialize repository: " + e.getMessage());
            return;
        }

        if (JoupenProperties.migrate) {
            new MigrationService(playerRepository).migrate();
        }

        new JoupenCommand(playerRepository, transactionManager);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository), this);

        EventUtils.register(PlayerProlongedEvent.class, new PlayerProlongedBroadcastListener());
        EventUtils.register(SendPrivateMessageEvent.class, new PrivateMessageListener());

        log.info("JoupenPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        log.info("JoupenPlugin disabling...");
        if (databaseManager != null) {
            databaseManager.close();
        }
        log.info("JoupenPlugin disabled!");
    }

    private void disable(String reason) {
        log.error(reason);
        getServer().getPluginManager().disablePlugin(this);
    }
}
