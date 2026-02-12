package org.joupen.service;

import lombok.extern.slf4j.Slf4j;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;

import java.util.List;
import java.util.Optional;

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
                Optional<PlayerEntity> playerInDb = playerRepository.findByUuid(playerInFile.getUuid());
                if (playerInDb.isPresent()) {
                    playerRepository.updateByUuid(playerInFile, playerInDb.get().getUuid());
                    log.info("Updated player in database: {}", playerInFile.getName());
                } else {
                    playerRepository.save(playerInFile);
                    log.info("Saved new player to database: {}", playerInFile.getName());
                }
            } catch (Exception e) {
                log.error("Failed to migrate player {} to database: {}", playerInFile.getName(), e.getMessage(), e);
            }
        }
        log.info("Migration from file to database completed.");
    }
}
