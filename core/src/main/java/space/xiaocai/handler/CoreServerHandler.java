package space.xiaocai.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import space.xiaocai.processor.FileProcessor;
import space.xiaocai.processor.FileGetProcessor;
import space.xiaocai.processor.FilePostProcessor;
import space.xiaocai.util.LogUtil;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 上传文件 curl
 * curl -v -x POST -H "Content-Type: multipart/form-data" -F "file=@/home/xiaokun/test.mp3" http://localhost:8080/upload
 */
public class CoreServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final String mPath;

    public CoreServerHandler(String path) {
        mPath = path;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            FileProcessor fileProcessor = null;

            HttpRequest httpRequest = (HttpRequest) msg;
            File file = new File(mPath);
            String currentUri = httpRequest.uri();
            LogUtil.logInfo("uri:%s", URLDecoder.decode(currentUri, StandardCharsets.UTF_8));

            String decode = URLDecoder.decode(currentUri, StandardCharsets.UTF_8);
            if (decode.startsWith("/")) {
                decode = decode.substring(1);
            }
            if (decode.endsWith("/")) {
                decode = decode.substring(0, decode.length() - 1);
            }
            if (decode.length() > 0) {
                file = new File(file, decode);
            }
            LogUtil.logInfo("method:%s", httpRequest.method().name());
            if (httpRequest.method() == HttpMethod.GET) {
                fileProcessor = new FileGetProcessor(httpRequest, ctx);
            } else if (httpRequest.method() == HttpMethod.POST) {
                fileProcessor = new FilePostProcessor(httpRequest, ctx);
            }
            if (fileProcessor != null) {
                fileProcessor.handleFile(file);
            }
        }
    }

}
