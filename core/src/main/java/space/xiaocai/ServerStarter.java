package space.xiaocai;

import static space.xiaocai.util.LogUtil.logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

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
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import space.xiaocai.db.DataManager;
import space.xiaocai.handler.ApiHandler;
import space.xiaocai.handler.AuthHandler;
import space.xiaocai.handler.CoreServerHandler;
import space.xiaocai.impl.ChatApiImpl;
import space.xiaocai.router.Router;
import space.xiaocai.util.LogUtil;

public class ServerStarter {
    private final String name;
    private final String password;
    private final int port;
    private final int bossThreads;
    private final int workerThreads;
    private final String path;
    private final Router router;
    private final Gson gson;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DataManager dataManager;

    public ServerStarter(StarterParams params) {
        this.name = params.getName();
        this.password = params.getPassword();
        this.port = params.getPort();
        this.bossThreads = params.getBossThreadCounts();
        this.workerThreads = params.getWorkerThreadCounts();
        path = params.getPath();
        initLog();
        initDatabase();
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
                .create();
        router = new Router(gson);
        router.register(new ChatApiImpl(gson, dataManager));
        initNetty();
    }

    private void initLog() {
        try {
            logger.setUseParentHandlers(false);
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            // 定义日志文件目录
            String logDirectory = path + File.separator + "logs";

            // 定义日志文件名前缀
            String logFileName = "server_log";

            // 定义日志文件后缀（可以根据需要添加时间戳等信息）
            String logFileExtension = ".log";

            // 获取当前时间，可以用于生成唯一的文件名
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String timestamp = dateFormat.format(currentDate);

            // 生成完整的日志文件名
            String fullLogFileName = logFileName + "_" + timestamp + logFileExtension;
            FileHandler handler = new FileHandler(logDirectory + File.separator + fullLogFileName, 10 * 1024 * 1024, 50, true);
            SimpleFormatter simpleFormatter = new SimpleFormatter() {
                private static final String format = "%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL : %2$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format, lr.getMillis(), lr.getMessage());
                }
            };
            handler.setFormatter(simpleFormatter);
            logger.addHandler(handler);
            logger.info("init log file suc");
        } catch (IOException e) {
            logger.warning("Failed to add file handler: " + e.getMessage());
        }
    }

    private void initDatabase() {
        dataManager = new DataManager();
    }

    private void initNetty() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        serverBootstrap = new ServerBootstrap();
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

                        //for cors
                        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();
                        pipeline.addLast(new CorsHandler(corsConfig));

                        pipeline.addLast(new ApiHandler(router, gson));
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
