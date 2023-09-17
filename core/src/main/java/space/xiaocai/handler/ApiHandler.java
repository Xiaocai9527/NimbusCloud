package space.xiaocai.handler;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import space.xiaocai.router.Router;
import space.xiaocai.util.LogUtil;

public class ApiHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Router router;
    private final Gson gson;

    public ApiHandler(Router router, Gson gson) {
        super(false);
        this.router = router;
        this.gson = gson;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        boolean filter = router.filter(msg);
        LogUtil.logInfo("request uri:%s; filter:%b", msg.uri(), filter);
        LogUtil.logInfo("request headers:%s", msg.headers().toString());
        if (filter) {
            ctx.fireChannelRead(msg);
        } else {
            try {
                Object result = router.dispatch(msg);
                if (result == null) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    ChannelFuture channelFuture = ctx.writeAndFlush(response);
                    if (!HttpUtil.isKeepAlive(msg)) {
                        channelFuture.addListener(ChannelFutureListener.CLOSE);
                    }
                } else {
                    String json = gson.toJson(result);
                    ByteBuf buf = Unpooled.wrappedBuffer(json.getBytes());
                    DefaultHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf, true);

                    response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                    String originDomain = msg.headers().get(HttpHeaderNames.ORIGIN);
                    if (!isEmpty(originDomain)) {
                        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, originDomain);
                        response.headers().set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
                    }
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
