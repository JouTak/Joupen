package commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.joupen.commands.BuildContext;
import org.joupen.commands.impl.LinkCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class LinkCommandTest {
    @Test
    void execute_shouldSendLink() {
        CommandSender sender = mock(CommandSender.class);
        BuildContext ctx = BuildContext.builder().sender(sender).args(new String[]{}).build();
        LinkCommand cmd = new LinkCommand(ctx);
        assertDoesNotThrow(cmd::execute);
        verify(sender, times(1)).sendMessage(any(TextComponent.class));
    }
}
