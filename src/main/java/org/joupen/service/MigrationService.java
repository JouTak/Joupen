package org.joupen.service;

import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.OperationMetadata;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;
import org.joupen.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
public class MigrationService {

    private final PlayerRepository playerRepository;

    public MigrationService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void migrate() {
        if (JoupenProperties.useSql) {
            migrateFromFileToDatabase();
        }
    }

    private void migrateFromFileToDatabase() {
        PlayerRepository fileRepository = new PlayerRepositoryFileImpl();
        List<PlayerEntity> playersInFile = fileRepository.findAll();

        if (playersInFile == null || playersInFile.isEmpty()) {
            log.info("No players found in file for migration.");
            return;
        }

        for (PlayerEntity playerInFile : playersInFile) {
            try {
                String externalId = UUID.nameUUIDFromBytes(Utils.toJson(playerInFile).getBytes(StandardCharsets.UTF_8)).toString();
                new PlayerOperationService(playerRepository).migratePlayer(playerInFile,
                        new OperationMetadata(JoupenProperties.playersFilepath, "migration", null, externalId));
                log.info("Migrated player {} to database", playerInFile.getName());
            } catch (Exception e) {
                log.error("Failed to migrate player {} to database: {}", playerInFile.getName(), e.getMessage(), e);
            }
        }
        log.info("Migration from file to database completed.");
    }
}
