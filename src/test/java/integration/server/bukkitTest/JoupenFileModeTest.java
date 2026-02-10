//package integration.server.bukkitTest;
//
//
//import integration.server.PurpurConfig;
//import org.junit.jupiter.api.*;
//
//import java.nio.file.Paths;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class JoupenFileModeTest extends BasePurpurTest {
//
//    @Override
//    protected PurpurConfig configurePurpur() {
//        PurpurConfig cfg = new PurpurConfig();
//        cfg.enableDiscordSrv = false; // без DiscordSRV
//        cfg.customConfigDir = Paths.get("src/test/resources/plugins/joupen/file-mode");
//        return cfg;
//    }
//
//    @Test
//    void testJoupenLoadsInFileMode() throws InterruptedException {
//        AtomicBoolean loaded = new AtomicBoolean(false);
//
//        purpur.followOutput(frame -> {
//            if (frame.getUtf8String().contains("JoupenPlugin enabled successfully!"))
//                loaded.set(true);
//        });
//
//        for (int i = 0; i < 60 && !loaded.get(); i++) Thread.sleep(1000);
//
//        Assertions.assertTrue(loaded.get(), "Joupen должен успешно загрузиться (file-mode)");
//    }
//}
