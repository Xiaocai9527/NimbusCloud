package server.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import util.LogUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final String userName;
    private final String password;

    public AuthHandler(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            if (noNeedAuth(request)) {
                ctx.fireChannelRead(request);
            } else {
                boolean authEmpty = isAuthEmpty(request);
                if (authEmpty) {
                    sendAuthenticationRequest(ctx);
                } else {
                    boolean authenticated = checkAuthentication(request);
                    if (authenticated) {
                        //如果身份验证成功，则将请求传递给下一个处理程序
                        ctx.fireChannelRead(request);
                    } else {
                        //如果身份验证失败，则返回错误响应
                        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            }
        } else {
            //如果不是HTTP请求，则直接传递给下一个处理程序
            ctx.fireChannelRead(msg);
        }
    }

    private boolean noNeedAuth(FullHttpRequest request) {
        String decode = URLDecoder.decode(request.uri(), StandardCharsets.UTF_8);
        boolean weddingRecord = decode.endsWith("wedding_record_low.mp4");
        boolean pdf = decode.endsWith(".pdf");
        boolean mp4 = decode.endsWith(".mp4");
        boolean png = decode.endsWith(".png");
        boolean apk = decode.endsWith(".apk");
        LogUtil.logInfo("weddingRecord:%b, pdf:%b ,mp4:%b", weddingRecord, pdf, mp4);
        return weddingRecord || pdf || mp4 || png || apk;
    }

    private boolean isAuthEmpty(FullHttpRequest request) {
        String authorization = request.headers().get(AUTHORIZATION_HEADER);
        return authorization == null || !authorization.startsWith("Basic ");
    }

    private boolean checkAuthentication(FullHttpRequest request) {
        String authorization = request.headers().get(AUTHORIZATION_HEADER);
        String credentials = authorization.substring(6);
        String decodedCredentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8);
        String[] parts = decodedCredentials.split(":", 2);
        boolean equalsName = userName.equals(parts[0]);
        boolean equalsPwd = password.equals(parts[1]);
        LogUtil.logInfo("equalsName:%b, equalsPwd:%b", equalsName, equalsPwd);
        return parts.length == 2 && equalsName && equalsPwd;
    }

    private void sendAuthenticationRequest(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
        response.headers().set("WWW-Authenticate", "Basic realm=\"Netty Http Server\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
