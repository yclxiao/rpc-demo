package com.baily.rpc.remote;

import com.baily.rpc.config.ServiceConfig;
import com.baily.rpc.rpc.RpcInvokeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.baily.rpc.common.Constants.DELIMITER;

/**
 * baily
 */
public class NettyServer {
    //注册服务
    private List<ServiceConfig> serviceConfigList;
    private Map<String, Method> interfaceMethodMap;

    public NettyServer(List<ServiceConfig> serviceConfigList, Map<String, Method> interfaceMethodMap) {
        this.serviceConfigList = serviceConfigList;
        this.interfaceMethodMap = interfaceMethodMap;
    }

    public int init(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
//                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 1024, DELIMITER));
                        channel.pipeline().addLast(new StringDecoder());
                        channel.pipeline().addLast(new RpcInvokeHandler(serviceConfigList, interfaceMethodMap));
                    }
                });
        ChannelFuture cf = bootstrap.bind(port).sync();
        System.out.println("启动NettyServer，端口为：" + port);
        return port;
    }
}
