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

        // Проверяем, включён ли плагин в конфигурации
        if (!JoutakProperties.enabled) {
            log.error("Plugin was disabled in config. Enable it in application.yml or application-{profile}.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Инициализация репозитория в зависимости от профиля
        try {
            // Инициализация DatabaseManager только для prod профиля
            if ("prod".equals(JoutakProperties.profile)) {
                databaseManager = new DatabaseManager();
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(databaseManager.getEntityManager());
            } else {
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(null); // Файловый репозиторий
            }
            log.info("Using profile: {} with repository: {}", JoutakProperties.profile, playerRepository.getClass().getSimpleName());

            // Перенос данных из JSON в базу, если профиль prod
            if ("prod".equals(JoutakProperties.profile)) {
                migratePlayersFromJsonToDatabase();
            }
        } catch (Exception e) {
            log.error("Failed to initialize repository: {}", e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Регистрация команд и событий
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

    private void migratePlayersFromJsonToDatabase() {
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