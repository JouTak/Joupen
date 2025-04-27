package org.joutak.loginpluginforjoutak;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.joutak.loginpluginforjoutak.commands.LoginAddAndRemovePlayerCommand;
import org.joutak.loginpluginforjoutak.database.DatabaseManager;
import org.joutak.loginpluginforjoutak.domain.PlayerEntity;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.joutak.loginpluginforjoutak.dto.PlayerDtos;
import org.joutak.loginpluginforjoutak.event.PlayerJoinEventHandler;
import org.joutak.loginpluginforjoutak.inputoutput.JsonReaderImpl;
import org.joutak.loginpluginforjoutak.inputoutput.JsonWriterImpl;
import org.joutak.loginpluginforjoutak.mapper.PlayerMapper;
import org.joutak.loginpluginforjoutak.repository.PlayerRepository;
import org.joutak.loginpluginforjoutak.repository.PlayerRepositoryFactory;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class LoginPluginForJoutak extends JavaPlugin {

    @Getter
    private static LoginPluginForJoutak instance;
    private JoutakProperties properties;
    private PlayerRepository playerRepository;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        properties = new JoutakProperties(this);

        if (!properties.enabled) {
            log.error("Plugin was disabled in config. Enable it in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            if (properties.useSql) {
                databaseManager = new DatabaseManager();
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(databaseManager.getEntityManager());
            } else {
                this.playerRepository = PlayerRepositoryFactory.getPlayerRepository(null);
            }
            log.info("Using profile with repository {}", playerRepository.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to initialize repository: {}", e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (properties.migrate) {
            if (properties.useSql) {
                migrateFromFileToDatabase();
            } else {
                migrateFromDatabaseToFile();
            }
        }

        new LoginAddAndRemovePlayerCommand(playerRepository);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinEventHandler(playerRepository), this);

        log.info("LoginPluginForJoutak enabled successfully!");
    }

    @Override
    public void onDisable() {
        log.info("LoginPluginForJoutak disabling...");
        if (databaseManager != null) {
            databaseManager.disconnect(databaseManager.getEntityManager());
            DatabaseManager.shutdown();
        }
        log.info("LoginPluginForJoutak disabled!");
    }

    private void migrateFromFileToDatabase() {
        if (!properties.useSql) {
            return;
        }
        JsonReaderImpl reader = new JsonReaderImpl(properties.playersFilepath);
        PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
        PlayerDtos players = reader.read();
        if (players == null || players.getPlayerDtoList() == null || players.getPlayerDtoList().isEmpty()) {
            log.info("No players found in file for migration.");
            return;
        }
        for (PlayerDto playerDto : players.getPlayerDtoList()) {
            try {
                Optional<PlayerEntity> existing = playerRepository.findByUuid(playerDto.getUuid());
                if (existing.isPresent()) {
                    playerMapper.updateEntityFromDto(playerDto, existing.get());
                    playerRepository.update(playerMapper.entityToDto(existing.get()));
                } else {
                    playerRepository.save(playerDto);
                }
                log.info("Migrated player from file to database: {} ({})", playerDto.getName(), playerDto.getUuid());
            } catch (Exception e) {
                log.warn("Failed to migrate player {} to database: {}", playerDto.getName(), e.getMessage());
            }
        }
        log.info("Migration from file to database completed.");
    }

    private void migrateFromDatabaseToFile() {
        if (properties.useSql) {
            return;
        }
        try {
            DatabaseManager tempDbManager = new DatabaseManager();
            PlayerRepository tempRepo = PlayerRepositoryFactory.getPlayerRepository(tempDbManager.getEntityManager());
            PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
            List<PlayerEntity> entities = tempRepo.findAll();
            if (entities.isEmpty()) {
                log.info("No players found in database for migration.");
                return;
            }
            PlayerDtos playerDtos = new PlayerDtos();
            playerDtos.setPlayerDtoList(entities.stream()
                    .map(playerMapper::entityToDto)
                    .collect(Collectors.toList()));
            JsonWriterImpl writer = new JsonWriterImpl(properties.playersFilepath);
            writer.write(playerDtos);
            log.info("Migration from database to file completed.");
            tempDbManager.disconnect(databaseManager.getEntityManager());
        } catch (Exception e) {
            log.error("Failed to migrate from database to file: {}", e.getMessage(), e);
        }
    }
}