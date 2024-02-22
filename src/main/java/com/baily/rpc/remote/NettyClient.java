package com.baily.rpc.remote;

import com.alibaba.fastjson.JSONObject;
import com.baily.rpc.common.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 * baily
 */
public class NettyClient {
    public ChannelHandlerContext ctx;
    private MessageCallback messageCallback;

    public NettyClient(String ip, int port) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
//                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 1024, Constants.DELIMITER));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new NettyClientHandler());

                            System.out.println("initChannel - " + Thread.currentThread().getName());
                        }
                    });
            ChannelFuture cf = bootstrap.connect(ip, port).sync();
//            cf.channel().closeFuture().sync();
            System.out.println("客户端启动成功");
        } catch (Exception e) {
            e.printStackTrace();
            // 出现异常时才需要优雅关闭。如果上来直接优化关闭，那就只能在channelActive里发送消息了，其余地方无法发送消息，因为通道已经被关闭了。
            // 至于服务端为什么只能收到channelReadComplete，而无法收到channelRead，是因为客户端发送消息时，通道已经关闭了。
            // 服务端收到的channelReadComplete，也只是因为客户端连接时产生的，而非发送具体消息时产生的。
            group.shutdownGracefully();
        } finally {
//            group.shutdownGracefully();
        }
    }

    public interface MessageCallback {
        void onMessage(String message);
    }

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public ChannelHandlerContext getCtx() throws InterruptedException {
        System.out.println("等待连接成功...");
        if (ctx == null) {
            synchronized (this) {
                wait();
            }
        }

        return ctx;
    }

    private class NettyClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                System.out.println("客户端收到消息：" + msg);
                String message = (String) msg;
                if (messageCallback != null) {
                    messageCallback.onMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            NettyClient.this.ctx = ctx;
            System.out.println("连接成功：" + ctx);
            synchronized (NettyClient.this) {
                NettyClient.this.notifyAll();
            }

        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }
    }

    public static void testSend(ChannelHandlerContext ctx) {
        JSONObject request = new JSONObject();
        //interface=com.ycl.prc.HelloService&method=sayHello&parameter=com.ycl.rpc.Student
        request.put("interfaces", "interface=com.ycl.rpc.HelloService&method=sayHello&parameter=com.ycl.rpc.Student");

        JSONObject bean = new JSONObject();
        bean.put("name", "不焦躁的程序员");
        bean.put("age", 30);

        JSONObject param = new JSONObject();
        param.put("com.ycl.rpc.Student", bean);

        request.put("parameter", param);

        request.put("requestId", String.valueOf(1));

        String requestStr = request.toJSONString() + Constants.DELIMITER_STR;
        System.out.println("发送给服务端JSON为：" + requestStr);
        ByteBuf byteBuf = Unpooled.copiedBuffer(requestStr.getBytes());
        ctx.writeAndFlush(byteBuf);
    }
}
