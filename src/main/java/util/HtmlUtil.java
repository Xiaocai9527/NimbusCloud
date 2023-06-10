package util;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HtmlUtil {

    public static String generate(@Nonnull File dir) {
        if (!dir.isDirectory()) {
            return Const.EMPTY;
        }
        String[] list = dir.list();
        if (list == null) {
            return Const.EMPTY;
        }
        //Arrays.stream(list).forEach(s -> LogUtil.logInfo("dir name:%s", s));
        return Const.HTML + getDirHtml(dir);
    }

    private static String getDirHtml(@Nonnull File folder) {
        File[] files = folder.listFiles();
        if (files == null) {
            return Const.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                String encodedFileName;
                encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                sb.append("<script>addRow(\"").append(fileName).append("\",\"")
                        .append(encodedFileName)
                        .append("\",false,\"")
                        .append(FileUtil.getFileSize(file))
                        .append("\",\"")
                        .append(FileUtil.getLastDateModified(file))
                        .append("\");</script>\n");
            } else if (file.isDirectory()) {
                String folderName = file.getName();
                String encodedFolderName;
                encodedFolderName = URLEncoder.encode(folderName, StandardCharsets.UTF_8);
                sb.append("<script>addRow(\"").append(folderName).append("\",\"")
                        .append(encodedFolderName)
                        .append("\",true,\"")
                        .append("--")
                        .append("\",\"")
                        .append(FileUtil.getLastDateModified(file))
                        .append("\");</script>\n");
            }
        }
        return sb.toString();
    }
}
