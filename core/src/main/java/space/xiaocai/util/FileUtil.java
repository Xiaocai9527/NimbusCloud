package space.xiaocai.util;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    private static final int KB = 1024;
    private static final int MB = 1024 * 1024;
    private static final int GB = 1024 * 1024 * 1024;

    public static String getFileSize(@Nonnull File file) {
        long length = file.length();
        double size;
        String unit;
        if (length < MB) {
            size = (double) length / KB;
            unit = "KB";
        } else if (length < GB) {
            size = (double) length / (MB);
            unit = "MB";
        } else {
            size = (double) length / (GB);
            unit = "GB";
        }
        return String.format("%.2f %s", size, unit);
    }

    public static String getLastDateModified(@Nonnull File file) {
        // 获取文件最后修改时间
        long lastModified = file.lastModified();
        // 将时间转换为日期格式
        Date date = new Date(lastModified);
        // 定义日期格式化字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        // 将日期格式化为字符串并返回
        return sdf.format(date);
    }
}
