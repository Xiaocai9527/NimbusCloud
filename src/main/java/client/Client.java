package client;

import okhttp3.*;
import util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Client {

    public static void main(String[] args) throws InterruptedException {
        OkHttpClient client = new OkHttpClient();
        File file = new File("/home/xiaokun/下载/linux_amd64.zip");
        long fileSize = file.length();
        byte[] buffer = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(buffer);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long partSize = 10 * 1024 * 1024; // 按照10MB分段上传
        long offset = 0;

        while (offset < fileSize) {
            long remainSize = fileSize - offset;
            long uploadSize = Math.min(partSize, remainSize);
            LogUtil.logInfo("remain size:%s; upload size:%s", remainSize, uploadSize);

            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), buffer, (int) offset, (int) uploadSize);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), body)
                    .build();

            Request request = new Request.Builder()
                    .url("http://127.0.0.1:8080/软件")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            offset += uploadSize;
            Thread.sleep(5000);
        }

    }
}
