package com.baily.rpc.demo;

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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.LongAdder;

/**
 * baily
 */
public class TestConsumer4NettyClient {

    public static void main(String[] args) {
        LongAdder longAdder = new LongAdder();
        int port = 50073;
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 1024, Constants.DELIMITER));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new NettyClientHandler(longAdder));
                        }
                    });
            ChannelFuture cf = bootstrap.connect(InetAddress.getLocalHost().getHostAddress(), port).sync();
            //如果没有cf.channel().closeFuture().sync(); 则一会儿就会进入到finally  group.shutdownGracefully();  则客户端关闭
            //cf.channel().closeFuture().sync(); 相当于主线程阻塞在这边，直接channel关闭，才进入到finally   group.shutdownGracefully();
            //如果没有 cf.channel().closeFuture().sync(); 则一会儿进程就结束了。没有这段代码的时候，可以启动一个线程while true循环，这样也不会关闭。

//            cf.channel().closeFuture().sync();
            System.out.println("提供者channel关闭");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class NettyClientHandler extends ChannelInboundHandlerAdapter {
        private LongAdder longAdder;

        public NettyClientHandler(LongAdder longAdder) {
            this.longAdder = longAdder;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);

            JSONObject request = new JSONObject();
            longAdder.increment();
            //interface=com.ycl.prc.HelloService&method=sayHello&parameter=com.ycl.rpc.Student
            request.put("interfaces", "interface=com.ycl.rpc.HelloService&method=sayHello&parameter=com.ycl.rpc.Student");

            JSONObject bean = new JSONObject();
            bean.put("name", "不焦躁的程序员");
            bean.put("age", 30);

            JSONObject param = new JSONObject();
            param.put("com.ycl.rpc.Student", bean);

            request.put("parameter", param);

            request.put("requestId", String.valueOf(longAdder.intValue()));

            String requestStr = request.toJSONString() + Constants.DELIMITER_STR;
            System.out.println("发送给服务端JSON为：" + requestStr);
            ByteBuf byteBuf = Unpooled.copiedBuffer(requestStr.getBytes());
            ctx.writeAndFlush(byteBuf);

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
