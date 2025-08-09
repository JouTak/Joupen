package org.joupen;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.joupen.commands.LoginAddAndRemovePlayerCommand;
import org.joupen.database.DatabaseManager;
import org.joupen.database.TransactionManager;
import org.joupen.domain.PlayerEntity;
import org.joupen.dto.PlayerDto;
import org.joupen.event.PlayerJoinEventHandler;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.PlayerRepositoryFactory;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;
import org.mapstruct.factory.Mappers;

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
        // Инициализация JoupenProperties
        try {
            JoupenProperties.initialize(this);
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
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(databaseManager, transactionManager);
            } else {
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(null, null);
            }
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

        new LoginAddAndRemovePlayerCommand(playerRepository, transactionManager);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository, transactionManager), this);

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
        PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
        List<PlayerEntity> players = fileRepository.findAll();
        if (players == null || players.isEmpty()) {
            log.info("No players found in file for migration.");
            return;
        }
        for (PlayerEntity playerEntity : players) {
            try {
                Optional<PlayerEntity> existing = playerRepository.findByUuid(playerEntity.getUuid());
                PlayerDto playerDto = playerMapper.entityToDto(playerEntity);
                if (existing.isPresent()) {
                    // Обновляем существующую сущность
                    playerRepository.update(playerDto);
                    log.info("Updated player in database: " + playerEntity.getName());
                } else {
                    // Сбрасываем ID для новой сущности
                    playerDto.setId(null);
                    playerRepository.save(playerDto);
                    log.info("Saved new player to database: {}", playerEntity.getName());
                }
            } catch (Exception e) {
                log.info("Failed to migrate player{}to database: {}", playerEntity.getName(), e.getMessage());
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
            PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
            List<PlayerEntity> entities = tempRepo.findAll();
            if (entities.isEmpty()) {
                log.info("No players found in database for migration.");
                return;
            }
            List<PlayerDto> playerDtos = entities.stream()
                    .map(playerMapper::entityToDto)
                    .toList();
            playerDtos.forEach(playerRepository::save);
            log.info("Migration from database to file completed.");
            tempDbManager.close();
        } catch (Exception e) {
            log.error("Failed to migrate from database to file: {} {}", e.getMessage(), e);
        }
    }
}