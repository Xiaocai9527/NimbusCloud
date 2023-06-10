package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;
import server.handler.AuthHandler;
import server.handler.CoreServerHandler;
import util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.*;

public class ServerStarter {
    private static final int PORT = 8080;
    // auth name
    private static final String USER_NAME = "";
    //auth pwd
    private static final String PASSWORD = "";

    public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);
        ServerBootstrap bootstrap = new ServerBootstrap();

        // you should return your file root path
        String path = getPath();

        try {
            logger.setUseParentHandlers(false);
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            logger.info("init log file");
            FileHandler handler = new FileHandler(path + File.separator + "日志/server_log.log", true);
            SimpleFormatter simpleFormatter = new SimpleFormatter() {
                private static final String format = "%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL : %2$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format, lr.getMillis(), lr.getMessage());
                }
            };
            handler.setFormatter(simpleFormatter);
            logger.addHandler(handler);
        } catch (IOException e) {
            logger.warning("Failed to add file handler: " + e.getMessage());
        }

        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(PORT))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(@NotNull SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            //http request/response content length 长度限制为 15MB
                            pipeline.addLast(new HttpObjectAggregator(15 * 1024 * 1024));
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new AuthHandler(USER_NAME, PASSWORD));
                            pipeline.addLast(new CoreServerHandler(path));
                        }
                    });

            ChannelFuture f = bootstrap.bind().sync();
            LogUtil.logInfo("server start..., port:%d", PORT);
            // closeFuture return ChannelFuture, when channel is closed, notify
            // sync, wait for this future until it is done
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static String getPath() {

        return "path";
    }
}
