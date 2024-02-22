package com.baily.rpc.demo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import static com.baily.rpc.common.Constants.DELIMITER;

/**
 * baily
 */
public class TestProducer4NettyServer {
    public static void main(String[] args) throws Exception {
        int port = 50073;
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
                        channel.pipeline().addLast(new NettyServerHandler());
                    }
                });
        ChannelFuture cf = bootstrap.bind(port).sync();
        System.out.println("启动NettyServer，端口为：" + port);
    }


    private static class NettyServerHandler extends ChannelInboundHandlerAdapter {

        public NettyServerHandler() {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            super.channelRead(ctx, msg);
            String message = (String) msg;
            System.out.println("消费者收到响应：" + message);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            System.out.println("消费者处理异常");
        }

    }
}
