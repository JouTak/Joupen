package org.joupen.commands.impl;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.joupen.commands.AbstractCommand;
import org.joupen.commands.BuildContext;
import org.joupen.commands.GameCommand;
import org.joupen.commands.JoupenCommandFactory;
import org.joupen.database.TransactionManager;
import org.joupen.repository.PlayerRepository;

@Slf4j
public class JoupenCommand extends AbstractCommand {

    private final PlayerRepository playerRepository;
    private final TransactionManager transactionManager;

    public JoupenCommand(PlayerRepository repo, TransactionManager tx) {
        super("joupen");
        this.playerRepository = repo;
        this.transactionManager = tx;
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        GameCommand gc = new JoupenCommandFactory().build(
                BuildContext.builder()
                        .sender(sender)
                        .label(label)
                        .argsTail(args)
                        .playerRepository(playerRepository)
                        .transactionManager(transactionManager)
                        .build()
        );
        gc.execute();
    }
}
