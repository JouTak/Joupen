package org.joupen.service;

import lombok.RequiredArgsConstructor;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joupen.enums.UUIDTypes.INITIAL_UUID;

@RequiredArgsConstructor
public class PlayerImportService {
    private final PlayerRepository repo;

    public List<PlayerEntity> buildNewPlayerFromFileWithNames(Path path, int days) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + path);
        }
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.ofDays(days);
        List<PlayerEntity> imported = new ArrayList<>();

        for (String raw : Files.readAllLines(path)) {
            String name = raw.trim();
            if (name.isEmpty()) continue;

            Optional<PlayerEntity> ex = repo.findByName(name);
            if (ex.isPresent()) continue;

            PlayerEntity entity = new PlayerEntity(name, true, INITIAL_UUID.getUuid(), now.plus(duration), now);

            imported.add(entity);
        }
        return imported;
    }
}
