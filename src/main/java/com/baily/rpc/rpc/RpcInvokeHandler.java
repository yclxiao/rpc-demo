package com.baily.rpc.rpc;

import com.alibaba.fastjson.JSONObject;
import com.baily.rpc.config.ServiceConfig;
import com.baily.rpc.rpc.RpcRequest;
import com.baily.rpc.rpc.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.baily.rpc.common.Constants.DELIMITER_STR;

/**
 * baily
 */
public class RpcInvokeHandler extends ChannelInboundHandlerAdapter {

    private Map<String, Method> interfaceMethodMap;
    private Map<Class, Object> interfaceInstanceMap = new ConcurrentHashMap<>();
    //创建线程池
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(100),
            new ThreadFactory() {
                AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "IO-thread-" + counter.incrementAndGet());
                }
            });

    public RpcInvokeHandler(List<ServiceConfig> serviceConfigList, Map<String, Method> interfaceMethodMap) {
        this.interfaceMethodMap = interfaceMethodMap;
        for (ServiceConfig serviceConfig : serviceConfigList) {
            this.interfaceInstanceMap.put(serviceConfig.getType(), serviceConfig.getInstance());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        System.out.println("提供者收到消息：" + message);
        //解析消费者发来的消息
        RpcRequest rpcRequest = RpcRequest.parse(message, ctx);
        //接受到消息，启动线程池处理消费者发过来的请求
        threadPoolExecutor.execute(new RpcInvokerTask(rpcRequest));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        System.out.println("进channelReadComplete了");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("提供者处理发生了异常：" + cause);
        ctx.close();
    }

    /**
     * 处理消费者发过来的请求
     */
    private class RpcInvokerTask implements Runnable {
        private RpcRequest rpcRequest;

        public RpcInvokerTask(RpcRequest rpcRequest) {
            this.rpcRequest = rpcRequest;
        }

        @Override
        public void run() {
            try {
                ChannelHandlerContext ctx = rpcRequest.getCtx();
                String interfaceIdentity = rpcRequest.getInterfaceIdentity();
                String requestId = rpcRequest.getRequestId();
                Map<String, Object> parameterMap = rpcRequest.getParameterMap();

                //interfaceIdentity组成：接口类+方法+参数类型
                Map<String, String> interfaceIdentityMap = string2Map(interfaceIdentity);

                //拿出是哪个类
                String interfaceName = interfaceIdentityMap.get("interface");
                Class interfaceClass = Class.forName(interfaceName);
                Object o = interfaceInstanceMap.get(interfaceClass);

                //拿出是哪个方法
                Method method = interfaceMethodMap.get(interfaceIdentity);

                //反射执行
                Object result = null;
                String parameterStr = interfaceIdentityMap.get("parameter");
                if (parameterStr != null && parameterStr.length() > 0) {
                    String[] parameterTypeClasses = parameterStr.split(",");//接口方法参数参数可能有多个，用,号隔开
                    Object[] parameterInstance = new Object[parameterTypeClasses.length];
                    for (int i = 0; i < parameterTypeClasses.length; i++) {
                        parameterInstance[i] = parameterMap.get(parameterTypeClasses[i]);
                    }
                    result = method.invoke(o, parameterInstance);
                } else {
                    result = method.invoke(o);
                }

                //将结果封装成rcpResponse
                RpcResponse rpcResponse = RpcResponse.create(JSONObject.toJSONString(result), interfaceIdentity, requestId);

                //ctx返回执行结果
                String resultStr = JSONObject.toJSONString(rpcResponse) + DELIMITER_STR;

                ByteBuf byteBuf = Unpooled.copiedBuffer(resultStr.getBytes());
                ctx.writeAndFlush(byteBuf);

                System.out.println("响应给客户端：" + resultStr);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    public static Map<String, String> string2Map(String str) {
        String[] split = str.split("&");
        Map<String, String> map = new HashMap<>();
        for (String s : split) {
            String[] split1 = s.split("=");
            map.put(split1[0], split1[1]);
        }
        return map;
    }

}
