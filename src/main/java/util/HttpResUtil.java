package util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HttpResUtil {
    private static final String SERVER_NAME = "Netty File Server";
    private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    public static void sendGetDirOk(File dir, ChannelHandlerContext ctx, HttpRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.copiedBuffer(HtmlUtil.generate(dir), CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response);
    }

    public static void sendGeneralOk(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendError(@Nonnull ChannelHandlerContext ctx, @Nonnull HttpResponseStatus status, @Nonnull String message) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.SERVER, SERVER_NAME);
        setDateAndCacheHeaders(response, new Date());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder();
        buf.append("<html><head><title>");
        buf.append(status.reasonPhrase());
        buf.append("</title></head><body>");
        buf.append("<h2>");
        buf.append(status.reasonPhrase());
        buf.append("</h2>");
        if (!message.isEmpty()) {
            buf.append("<p>");
            buf.append(message);
            buf.append("</p>");
        }
        buf.append("</body></html>");

        ByteBuf content = ctx.alloc().buffer(buf.length());
        content.writeBytes(buf.toString().getBytes(StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.writeAndFlush(response);
    }

    private static void setDateAndCacheHeaders(@Nonnull HttpResponse response, @Nonnull Date lastModified) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        HttpHeaders headers = response.headers();
        headers.set(HttpHeaderNames.DATE, dateFormat.format(new Date()));
        headers.set(HttpHeaderNames.EXPIRES, dateFormat.format(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)));
        headers.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=86400");
        headers.set(HttpHeaderNames.LAST_MODIFIED, dateFormat.format(lastModified));
    }
}
