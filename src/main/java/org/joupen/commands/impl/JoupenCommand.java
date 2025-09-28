package org.joupen.commands.impl;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.joupen.commands.AbstractCommand;
import org.joupen.commands.BuildContext;
import org.joupen.commands.GameCommand;
import org.joupen.commands.JoupenCommandFactory;
import org.joupen.database.TransactionManager;
import org.joupen.events.publishers.SimpleEventBus;
import org.joupen.mapper.PlayerMapper;
import org.joupen.repository.PlayerRepository;
import org.mapstruct.factory.Mappers;

@Slf4j
public class JoupenCommand extends AbstractCommand {

    private final PlayerRepository playerRepository;
    private final TransactionManager transactionManager;
    private final PlayerMapper playerMapper;

    public JoupenCommand(PlayerRepository repo, TransactionManager tx) {
        super("joupen");
        this.playerRepository = repo;
        this.transactionManager = tx;
        this.playerMapper = Mappers.getMapper(PlayerMapper.class);
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        GameCommand gc = new JoupenCommandFactory().build(
                BuildContext.builder()
                        .sender(sender)
                        .label(label)
                        .argsTail(args)
                        .simpleEventBus(new SimpleEventBus())
                        .playerRepository(playerRepository)
                        .playerMapper(playerMapper)
                        .transactionManager(transactionManager)
                        .build()
        );
        gc.execute();
    }
}
