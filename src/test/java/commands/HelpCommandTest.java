package commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.impl.HelpCommand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HelpCommandTest {
    @Test
    void execute_shouldSendHelpMessage() {
        CommandSender sender = mock(CommandSender.class);
        BuildContext ctx = BuildContext.builder().sender(sender).args(new String[]{}).build();
        HelpCommand cmd = new HelpCommand(ctx);
        assertDoesNotThrow(cmd::execute);
        verify(sender, times(1)).sendMessage(any(TextComponent.class));
    }
}
