package integration.server;

import java.nio.file.Path;

public class PurpurConfig {
    public boolean enableDiscordSrv = false;
    public Path customConfigDir = null;
    public String discordVersion = "v1.27.0";
    public String joupenJarPath = "target/Joupen-2.1.0.jar";
    public boolean useSql = false;
    public String mariaDbUrl = null;
    public String mariaDbUser = "user";
    public String mariaDbPassword = "user_password";
}
