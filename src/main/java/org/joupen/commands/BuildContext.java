package org.joupen.commands;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.joupen.database.TransactionManager;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerService;

@Getter
@Builder
public class BuildContext {
    private final CommandSender sender;
    private final String label;
    private final String[] argsTail;
    private final PlayerRepository playerRepository;
    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final TransactionManager transactionManager;
}
