package space.xiaocai;

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
import space.xiaocai.handler.AuthHandler;
import space.xiaocai.handler.CoreServerHandler;
import space.xiaocai.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.*;

public class ServerStarter {
    public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final String name;
    private final String password;
    private final int port;
    private final int bossThreads;
    private final int workerThreads;
    private final String path;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public ServerStarter(StarterParams params) {
        this.name = params.getName();
        this.password = params.getPassword();
        this.port = params.getPort();
        this.bossThreads = params.getBossThreadCounts();
        this.workerThreads = params.getWorkerThreadCounts();
        path = params.getPath();
        init();
    }

    private void init() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        serverBootstrap = new ServerBootstrap();

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
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(@NotNull SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        //http request/response content length 长度限制为 15MB
                        pipeline.addLast(new HttpObjectAggregator(15 * 1024 * 1024));
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new AuthHandler(name, password));
                        pipeline.addLast(new CoreServerHandler(path));
                    }
                });
    }

    public void start() {
        try {
            ChannelFuture f = serverBootstrap.bind().sync();
            LogUtil.logInfo("server start..., port:%d", port);
            // closeFuture return ChannelFuture, when channel is closed, notify
            // sync, wait for this future until it is done
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            stop();
        }
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
