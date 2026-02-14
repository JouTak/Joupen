//package integration.server;
//
//
//import integration.server.bukkitTest.BasePurpurTest;
//import org.junit.jupiter.api.*;
//
//import java.nio.file.Paths;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class DiscordIntegrationTest extends BasePurpurTest {
//
//    @Override
//    protected PurpurConfig configurePurpur() {
//        PurpurConfig cfg = new PurpurConfig();
//        cfg.enableDiscordSrv = true;
//        cfg.customConfigDir = Paths.get("src/test/resources/plugins/joupen/sql-mode");
//        return cfg;
//    }
//
//    @Test
//    void testDiscordSrvLoads() throws InterruptedException {
//        AtomicBoolean discordLoaded = new AtomicBoolean(false);
//
//        purpur.followOutput(frame -> {
//            if (frame.getUtf8String().contains("[DiscordSRV] Enabling DiscordSRV"))
//                discordLoaded.set(true);
//        });
//
//        for (int i = 0; i < 60 && !discordLoaded.get(); i++) Thread.sleep(1000);
//
//        Assertions.assertTrue(discordLoaded.get(), "DiscordSRV должен загрузиться");
//    }
//}
