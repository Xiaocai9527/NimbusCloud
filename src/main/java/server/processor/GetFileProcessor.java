package server.processor;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import org.jetbrains.annotations.NotNull;
import util.HttpResUtil;
import util.LogUtil;
import util.MediaResourceUtil;
import util.QRCodeGenerator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GetFileProcessor implements FileProcessor {

    private static final Pattern pattern = Pattern.compile("\\.log(\\.\\d+)?$");
    private final HttpRequest request;
    private final ChannelHandlerContext ctx;
    private final String currentUri;
    private final String host;

    public GetFileProcessor(HttpRequest request, ChannelHandlerContext ctx) {
        this.request = request;
        this.ctx = ctx;
        this.currentUri = request.uri();
        HttpHeaders headers = request.headers();
        String mReferrer = headers.get(HttpHeaderNames.REFERER);
        this.host = headers.get(HttpHeaderNames.HOST);
        LogUtil.logInfo("request headers:%s", headers.toString());
        LogUtil.logInfo("mHost:%s , referrer:%s", host, mReferrer);
    }

    @Override
    public void handleFile(File file) throws IOException {
        if (!file.exists()) {
            HttpResUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, "file not found");
            return;
        }
        LogUtil.logInfo("file is dir:%b", file.isDirectory());
        if (file.isDirectory()) {
            handleDir(file, ctx);
        } else {
            handleFile(file, ctx);
        }
    }

    private void handleDir(File dir, ChannelHandlerContext ctx) {
        try (Stream<Path> paths = Files.list(Paths.get(dir.getPath()))) {
            paths.filter(path -> path.toString().endsWith(".apk"))
                    .forEach(path -> {
                        String replace = path.getFileName().toString().replace(".apk", ".png");
                        try {
                            URI uri = new URI("http://" + host + currentUri + path.getFileName());
                            QRCodeGenerator.generateQRCode(uri.toString(), path + currentUri + replace);
                        } catch (URISyntaxException e) {
                            LogUtil.logError("uri error:%s", e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LogUtil.logError("handleDir generate qrcode error :%s", e.getMessage());
        }

        HttpResUtil.sendGetDirOk(dir, ctx, request);
    }

    private void handleFile(File file, ChannelHandlerContext ctx) throws IOException {
        // 针对 文件 和 流媒体作区分处理
        String name = file.getName();
        // 如果流媒体文件不使用 PARTIAL_CONTENT, 那么浏览器只会先把文件整体都下载下来才开始播放。
        // 此时浏览器不支持随意的seek 播放
        if (name.endsWith(".mp4") || name.endsWith(".mp3")) {
            MediaResourceUtil.useRandomAccessFile(request, file, ctx);
        } else {
            // 使用RandomAccessFile而不是直接使用File主要是为了支持文件随机访问。File是Java中用于表示文件和目录路径名的类，
            // 它只提供了一些基本的文件操作方法，如文件创建、删除、重命名等，但是它不能直接访问文件的内容。
            // 而RandomAccessFile是一个可以支持文件随机访问的类，它提供了一些方法，如seek()、read()、write()等，
            // 可以在文件中定位、读取、写入数据。在这段代码中，使用RandomAccessFile来读取文件内容，可以更加灵活地控制文件的读取和传输，以及支持大文件的传输。
            RandomAccessFile raf = null;
            long fileLength = -1;
            try {
                raf = new RandomAccessFile(file, "r");
                fileLength = raf.length();
            } catch (Exception e) {
                HttpResUtil.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, " RandomAccessFile file error");
                return;
            } finally {
                if (fileLength < 0 && raf != null) {
                    raf.close();
                }
            }
            // 注意这里不要写成了 DefaultFullHttpResponse ，
            // DefaultFullHttpResponse 和 DefaultHttpResponse 区别
            /**
             * DefaultHttpResponse 是一个不带有消息体的 HTTP 响应消息。它包括 HTTP 版本、状态码、响应头等信息，但不包括消息体。通常在 HTTP GET 请求的响应中使用。
             * DefaultFullHttpResponse 则是一个带有消息体的 HTTP 响应消息。除了包含 HTTP 版本、状态码、响应头等信息外，还包括响应的消息体。通常在 HTTP POST 请求的响应中使用。
             * 因此，DefaultFullHttpResponse 比 DefaultHttpResponse 更加完整，能够包含更多的信息。
             */
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            String guessName = URLConnection.guessContentTypeFromName(name);
            LogUtil.logInfo("guessName:%s", guessName);
            if (name.endsWith(".cpp")) {
                guessName = "text/x-cpp-source";
            } else if (name.endsWith(".java")) {
                guessName = "text/x-java-source";
            } else if (name.endsWith(".gradle")) {
                guessName = "text/x-gradle-source";
            } else if (name.endsWith(".log") || pattern.matcher(name).find()) {
                guessName = "text/x-log-source";
            } else if (name.endsWith(".sh")) {
                guessName = "text/x-sh-source";
            }
            response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                    Objects.requireNonNullElse(guessName, "application/octet-stream"));

//            if (name.endsWith(".pdf")) {
//                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/pdf");
//                //在浏览器中打开PDF文件，可以将Content-Disposition属性设置为"inline"
//                response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"");
//            } else {
//                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
//                //值为"attachment; filename=xxx"，这意味着告诉客户端将响应正文作为附件进行下载，而不是直接在浏览器中打开
//                response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
//            }

            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            ctx.write(response);

            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory(); // 获取JVM当前的总内存
            long freeMemory = runtime.freeMemory(); // 获取JVM当前的空闲内存
            LogUtil.logInfo("totalMemory:%s ; freeMemory:%s", totalMemory, freeMemory);
            ChannelFuture sendFileFuture;
            if (ctx.pipeline().get(SslHandler.class) == null) {
                // SSL not enabled - can use zero-copy file transfer.
                // DefaultFileRegion可以更可靠地传输大文件，并提供了进度反馈功能。
                // DefaultFileRegion支持传输任意大小的文件，因为它可以将文件映射到内存中，
                // 而不是将整个文件读取到内存中，从而避免了内存溢出的风险。
                sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            } else {
                // ssl enabled 的时候, 数据会加密, 无法做到零拷贝
                // SSL enabled - cannot use zero-copy file transfer.
                // 使用 chunkedFile, chunkedFile 特点是分段传输,
                // response header need add transfer-encoding: chunked
                ChunkedFile chunkedFile = new ChunkedFile(raf);
                HttpChunkedInput chunkedInput = new HttpChunkedInput(chunkedFile);
                sendFileFuture = ctx.write(chunkedInput, ctx.newProgressivePromise());
            }

//                if (file.fileLength() < freeMemory / 8) {
//                    //DefaultFileRegion可以更可靠地传输大文件，并提供了进度反馈功能。
//                    //DefaultFileRegion支持传输任意大小的文件，因为它可以将文件映射到内存中，而不是将整个文件读取到内存中，从而避免了内存溢出的风险。
//                    sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
//                } else {
//                    ChunkedFile chunkedFile = new ChunkedFile(raf);
//                    HttpChunkedInput chunkedInput = new HttpChunkedInput(chunkedFile);
//                    sendFileFuture = ctx.write(chunkedInput, ctx.newProgressivePromise());
//                }

            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    if (total < 0) {
                        // Total file size is unknown
                        LogUtil.logInfo("Transfer progress: %s", progress);
                    } else {
                        LogUtil.logInfo("Transfer progress: %s / %s", progress, total);
                    }
                }

                @Override
                public void operationComplete(@NotNull ChannelProgressiveFuture future) throws Exception {
                    LogUtil.logInfo("Transfer complete.");
                }
            });

            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!HttpUtil.isKeepAlive(request)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
