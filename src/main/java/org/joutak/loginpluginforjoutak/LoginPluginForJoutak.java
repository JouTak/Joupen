package org.joutak.loginpluginforjoutak;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.joutak.loginpluginforjoutak.commands.LoginAddAndRemovePlayerCommand;
import org.joutak.loginpluginforjoutak.database.DatabaseManager;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.event.PlayerJoinEventHandler;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.joutak.loginpluginforjoutak.repository.PlayerRepositoryFactory;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;

@Slf4j
public final class LoginPluginForJoutak extends JavaPlugin {

    @Getter
    private static LoginPluginForJoutak instance;
    private PlayerRepository playerRepository;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!JoutakProperties.enabled) {
            log.error("Plugin was disabled in config. Enable it in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            if (JoutakProperties.useSql) {
                databaseManager = new DatabaseManager();
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(databaseManager.getEntityManager());
                if (!isDatabaseEmpty()) {
                    log.info("Database has players, skipping JSON migration");
                } else {
                    migratePlayersFromJsonToDatabase();
                }
            } else {
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(null);
            }
            log.info("Using profile  with repository {}",
                    playerRepository.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to initialize repository: {}", e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new LoginAddAndRemovePlayerCommand();
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository), this);

        log.info("LoginPluginForJoutak enabled successfully!");
    }

    @Override
    public void onDisable() {
        log.info("LoginPluginForJoutak disabling...");
        if (databaseManager != null) {
            databaseManager.disconnect();
            DatabaseManager.shutdown();
        }
        log.info("LoginPluginForJoutak disabled!");
    }

    private boolean isDatabaseEmpty() {
        if (!JoutakProperties.useSql || playerRepository == null) {
            return true;
        }
        try {
            return playerRepository.findAll().isEmpty();
        } catch (Exception e) {
            log.warn("Failed to check database emptiness: {}", e.getMessage());
            return true;
        }
    }

    private void migratePlayersFromJsonToDatabase() {
        if (!JoutakProperties.useSql) {
            return;
        }

        JsonReaderImpl reader = new JsonReaderImpl(JoutakProperties.saveFilepath);
        PlayerDtos players = reader.read();

        if (players == null || players.getPlayerDtoList() == null || players.getPlayerDtoList().isEmpty()) {
            log.warn("No players found in {}", JoutakProperties.saveFilepath);
            return;
        }

        for (PlayerDto player : players.getPlayerDtoList()) {
            try {
                playerRepository.save(player);
                log.info("Migrated player: {} ({})", player.getName(), player.getUuid());
            } catch (Exception e) {
                log.warn("Failed to migrate player {}: {}", player.getName(), e.getMessage());
            }
        }

        log.info("Player migration from JSON to MariaDB completed.");
    }
}