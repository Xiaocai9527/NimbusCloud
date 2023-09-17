import space.xiaocai.ServerStarter;
import space.xiaocai.StarterParams;
import space.xiaocai.configs.AppConfig;
import space.xiaocai.util.LogUtil;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        AppConfig appConfig = AppConfig.getInstance();
        StarterParams params = new StarterParams.Builder()
                .name(appConfig.getWebName())
                .password(appConfig.getWebPwd())
                .path(getPath())
                .build();
        ServerStarter starter = new ServerStarter(params);
        starter.start();
    }

    // you should return your file root path
    public static String getPath() {
        String path = "/media/xiaokun/Elements2";
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            LogUtil.logInfo("win");
            path = "G:\\";
        } else if (osName.contains("mac")) {
            LogUtil.logInfo("mac");
            path = "/Users/xiaokun_mac";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            LogUtil.logInfo("linux");
            // 创建一个File对象来表示路径
            File dir = new File(path);

            // 检查路径是否存在并且是一个目录
            if (dir.exists() && dir.isDirectory()) {
                // 检查是否包含名为"ai"的文件夹
                File aiDir = new File(dir, "ai");

                if (!aiDir.exists() || !aiDir.isDirectory()) {
                    // 如果不存在"ai"文件夹，或者"ai"不是一个目录，将路径更改为"/media/xiaokun/Elements1"
                    path = "/media/xiaokun/Elements1";
                }
            } else {
                // 如果路径不存在或者不是一个目录，将路径更改为"/media/xiaokun/Elements1"
                path = "/media/xiaokun/Elements1";
            }
        } else {
            LogUtil.logInfo("unKnown sys os");
        }
        return path;
    }
}
