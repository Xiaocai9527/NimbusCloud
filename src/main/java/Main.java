import space.xiaocai.ServerStarter;
import space.xiaocai.StarterParams;
import space.xiaocai.util.LogUtil;

public class Main {

    public static void main(String[] args) {
        StarterParams params = new StarterParams.Builder()
                .path(getPath())
                .build();
        ServerStarter starter = new ServerStarter(params);
        starter.start();
    }

    // you should return your file root path
    public static String getPath() {
        String path = "/media/xiaokun/Elements";
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            LogUtil.logInfo("win");
            path = "G:\\";
        } else if (osName.contains("mac")) {
            LogUtil.logInfo("mac");
            path = "/Users/xiaokun_mac";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            LogUtil.logInfo("linux");
            path = "/media/xiaokun/Elements2";
        } else {
            LogUtil.logInfo("unKnown sys os");
        }
        return path;
    }
}
