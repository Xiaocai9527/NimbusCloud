package space.xiaocai.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import space.xiaocai.util.HttpResUtil;
import space.xiaocai.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;

public class FilePostProcessor implements FileProcessor {

    private final HttpRequest request;
    private final ChannelHandlerContext ctx;

    public FilePostProcessor(HttpRequest request, ChannelHandlerContext ctx) {
        this.request = request;
        this.ctx = ctx;
    }


    @Override
    public void handleFile(File file) throws IOException {
        uploadFile(file, ctx, request);
    }

    private void uploadFile(File dir, ChannelHandlerContext ctx, HttpRequest msg) throws IOException {
        LogUtil.logInfo("uploadFile");
        if (msg.headers().contains(HttpHeaderNames.EXPECT)) {
            if (msg.headers().get(HttpHeaderNames.EXPECT).equalsIgnoreCase(HttpHeaderValues.CONTINUE.toString())) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            } else {
                HttpResUtil.sendError(ctx, HttpResponseStatus.EXPECTATION_FAILED, "Expectation Failed");
                return;
            }
        }

        if (!msg.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            HttpResUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Missing Content-Type header.");
            return;
        }

        String contentType = msg.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (!contentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
            HttpResUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid Content-Type header.");
            return;
        }

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(), msg);

        try {
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attribute = (Attribute) data;
                    LogUtil.logInfo("Attribute: %s = %s", attribute.getName(), attribute.getValue());
                } else if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    FileUpload fileUpload = (FileUpload) data;
                    LogUtil.logInfo("FileUpload: %s = %s", fileUpload.getName(), fileUpload.getFilename());
                    if (fileUpload.isCompleted()) {
                        saveFileUpload(dir, fileUpload);
                    }
                }
            }
        } catch (IOException e) {
            HttpResUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid HTTP request.");
            return;
        }
        if (decoder.isMultipart() && !msg.decoderResult().isFailure()) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            HttpResUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid HTTP request.");
        }
        decoder.destroy();
    }

    private void saveFileUpload(File dir, FileUpload fileUpload) throws IOException {
        String fileName = fileUpload.getFilename();
        String decodedFileName = URLDecoder.decode(fileName, CharsetUtil.UTF_8);

        LogUtil.logInfo("saveFileUpload decodedFileName:%s; length: %s; isInMemory: %b",
                decodedFileName, fileUpload.length(), fileUpload.isInMemory());

        File file = new File(dir, decodedFileName);

        // TODO: 2023/6/27
        // 需要优化, 上传文件需要对比 md5;
        // 如果上传来的文件, 和本地文件的 md5 已经是一致的，表明文件服务器上已经存在,
        // 则返回错误码告诉客户端文件已存在；

        if (fileUpload.length() == 0) {
            boolean newFile = file.createNewFile();
            LogUtil.logInfo("saveFileUpload createNewFile:%b", newFile);
        } else {
            LogUtil.logInfo("fileUpload isInMemory:%b", fileUpload.isInMemory());
            if (fileUpload.isInMemory()) {
                byte[] bytes = fileUpload.get();
                LogUtil.logInfo("file.length:%s,bytes:%s", file.length(), bytes.length);
                try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                    outputStream.write(bytes);
                }
            } else {
                try (RandomAccessFile inputFile = new RandomAccessFile(fileUpload.getFile(), "r");
                     FileChannel inputChannel = inputFile.getChannel();
                     RandomAccessFile outputFile = new RandomAccessFile(file, "rw");
                     FileChannel outputChannel = outputFile.getChannel()) {
                    LogUtil.logInfo("output file start position:%s", outputFile.length());
                    outputChannel.transferFrom(inputChannel, outputFile.length(), inputChannel.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        LogUtil.logInfo("File uploaded: %s", file.getAbsoluteFile());
    }
}
