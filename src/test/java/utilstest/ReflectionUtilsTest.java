package utilstest;

import org.joupen.commands.GameCommand;
import org.joupen.utils.ReflectionUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReflectionUtilsTest {

    @Test
    public void testFindClassesImplementsInterfaceGameCommand() {
        Set<Class<? extends GameCommand>> classes = ReflectionUtils.findClassesImplementsInterfaceGameCommand();
        assertEquals(6, classes.size());
    }
}
