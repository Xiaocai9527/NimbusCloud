package space.xiaocai.util;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_RANGE;

public class MediaResourceUtil {

    public static void useBufferInputStream(HttpRequest request, File file, ChannelHandlerContext ctx) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            HttpHeaders headers = request.headers();
            LogUtil.logInfo("handleFile target file length:%s; contains range:%b",
                    file.length(), headers.contains(HttpHeaderNames.RANGE));
            long length = file.length();
            long requestSize = length;
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/mp4");
            response.headers().set(HttpHeaderNames.ACCEPT_RANGES, "bytes");
            if (headers.contains(HttpHeaderNames.RANGE)) {
                String range = headers.get(HttpHeaderNames.RANGE);
                String[] ranges = range.substring("bytes=".length()).split("-");
                LogUtil.logInfo("range:%s", range);
                long start = Long.parseLong(ranges[0]);
                long end = range.endsWith("-") ? length - 1 : Long.parseLong(ranges[1]);
                requestSize = (end - start) + 1;
                response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
                response.headers().set(CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
                response.headers().set(CONTENT_LENGTH, requestSize);

                in.skip(start);
                ctx.write(response);
                ctx.write(new ChunkedStream(new LimitedInputStream(in, requestSize)), ctx.newProgressivePromise());
            } else {
                ctx.write(response);
                ctx.write(new ChunkedStream(new LimitedInputStream(in, length)), ctx.newProgressivePromise());
            }

            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!HttpUtil.isKeepAlive(request)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    public static void useRandomAccessFile(HttpRequest request, File file, ChannelHandlerContext ctx) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            HttpHeaders headers = request.headers();
            LogUtil.logInfo("handleFile target file length:%s; headers:%s; contains range:%b",
                    raf.length(), headers.toString(), headers.contains(HttpHeaderNames.RANGE));
            long length = raf.length();
            long requestSize = length;
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, raf.length());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/mp4");
            response.headers().set(HttpHeaderNames.ACCEPT_RANGES, "bytes");
            if (headers.contains(HttpHeaderNames.RANGE)) {
                String range = headers.get(HttpHeaderNames.RANGE);
                String[] ranges = range.substring("bytes=".length()).split("-");
                LogUtil.logInfo("range:%s", range);
                long start = Long.parseLong(ranges[0]);
                long end = range.endsWith("-") ? length - 1 : Long.parseLong(ranges[1]);
                requestSize = (end - start) + 1;
                LogUtil.logInfo("start:%s ; end:%s; requestSize:%s", start, end, requestSize);
                response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
                response.headers().set(CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
                response.headers().set(CONTENT_LENGTH, requestSize);

                raf.seek(start);
                ctx.write(response);
                ChannelFuture sendFileFuture = ctx.writeAndFlush(new DefaultFileRegion(raf.getChannel(), start, requestSize), ctx.newProgressivePromise());
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
               // ctx.write(new ChunkedFile(raf, start, requestSize, 8192), ctx.newProgressivePromise());
                LogUtil.logInfo("response.headers():%s; response.getStatus():%s", response.headers().toString(), response.getStatus());
            } else {
                LogUtil.logInfo("headers not contains range");
                ctx.write(response);
                ctx.writeAndFlush(new ChunkedFile(raf, 0, length, 8192), ctx.newProgressivePromise());
            }

            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!HttpUtil.isKeepAlive(request)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
