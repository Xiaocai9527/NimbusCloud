package space.xiaocai.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import static space.xiaocai.ServerStarter.logger;

public class LogUtil {

    public static void logInfo(String info, Object... args) {
        String timePrefix = getTimePrefix();
        if (args == null || args.length == 0) {
            logger.info(info);
            //for local log debug
            System.out.println(timePrefix + info);
        } else {
            logger.info(String.format(info, args));
            System.out.printf(timePrefix + info, args).println();
        }
    }

    public static void logError(String error, Object... args) {
        String timePrefix = getTimePrefix();
        if (args == null || args.length == 0) {
            logger.warning(error);
            System.err.println(timePrefix + error);
        } else {
            logger.warning(String.format(error, args));
            System.err.printf(timePrefix + error, args).println();
        }
    }

    private static String getTimePrefix() {
        long millis = System.currentTimeMillis();
        Date date = new Date(millis);
        // 定义日期格式化字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS : ");
        // 将日期格式化为字符串并返回
        return sdf.format(date);
    }

}
