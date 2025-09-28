package org.joupen;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.joupen.commands.impl.JoupenCommand;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.event.PlayerJoinEventHandler;
import org.joupen.events.PlayerProlongedEvent;
import org.joupen.events.listeners.PlayerProlongedBroadcastListener;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.PlayerRepositoryFactory;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.EventUtils;
import org.joupen.utils.JoupenProperties;

import java.util.List;
import java.util.Optional;

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
        EventUtils.register(PlayerProlongedEvent.class, new PlayerProlongedBroadcastListener());
        try {
            JoupenProperties.initialize(this.getDataFolder());
        } catch (Exception e) {
            log.info("Failed to initialize JoupenProperties: {} {}", e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!JoupenProperties.enabled) {
            log.info("Plugin was disabled in config. Enable it in config.yml");
            getServer().getPluginManager().disablePlugin(this);
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
            log.info("Failed to initialize repository: {} {}", e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (JoupenProperties.migrate) {
            if (JoupenProperties.useSql) {
                migrateFromFileToDatabase();
            } else {
                migrateFromDatabaseToFile();
            }
        }

        new JoupenCommand(playerRepository, transactionManager);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository), this);

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

    private void migrateFromFileToDatabase() {
        if (!JoupenProperties.useSql) {
            return;
        }
        PlayerRepository fileRepository = new PlayerRepositoryFileImpl();
        List<PlayerEntity> playersInFile = fileRepository.findAll();

        if (playersInFile == null || playersInFile.isEmpty()) {
            log.info("No players found in file for migration.");
            return;
        }

        for (PlayerEntity playerInFile : playersInFile) {
            try {
                Optional<PlayerEntity> playerInDb = playerRepository.findByUuid(playerInFile.getUuid());
                if (playerInDb.isPresent()) {
                    // Обновляем существующую сущность
                    playerRepository.updateByUuid(playerInFile, playerInDb.get().getUuid());
                    log.info("Updated player in database: {}", playerInFile.getName());
                } else {
                    playerRepository.save(playerInFile);
                    log.info("Saved new player to database: {}", playerInFile.getName());
                }
            } catch (Exception e) {
                log.info("Failed to migrate player{}to database: {}", playerInFile.getName(), e.getMessage());
            }
        }
        log.info("Migration from file to database completed.");
    }

    private void migrateFromDatabaseToFile() {
        if (JoupenProperties.useSql) {
            return;
        }
        try {
            // Создаём временный DB-репозиторий для чтения из базы данных
            DatabaseManager tempDbManager = new DatabaseManager();
            TransactionManager tempTransactionManager = new TransactionManager(tempDbManager);
            PlayerRepository tempRepo = PlayerRepositoryFactory.getPlayerRepository(tempDbManager, tempTransactionManager);
            List<PlayerEntity> entities = tempRepo.findAll();
            if (entities.isEmpty()) {
                log.info("No players found in database for migration.");
                return;
            }
            entities.forEach(playerRepository::save);
            log.info("Migration from database to file completed.");
            tempDbManager.close();
        } catch (Exception e) {
            log.error("Failed to migrate from database to file: {} {}", e.getMessage(), e);
        }
    }
}