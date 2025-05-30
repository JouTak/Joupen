package org.joupen.commands;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.joupen.JoupenPlugin;


public abstract class AbstractCommand implements CommandExecutor {

    public AbstractCommand(String command) {
        PluginCommand pluginCommand = JoupenPlugin.getInstance().getCommand(command);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
        }
    }

    public abstract void execute(CommandSender commandSender, Command command, String string, String[] args);

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] args) {
        execute(commandSender, command, string, args);
        return true;
    }
}
