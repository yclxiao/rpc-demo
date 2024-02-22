package com.baily.rpc.cluster;

import com.alibaba.fastjson.JSONObject;
import com.baily.rpc.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * baily
 */
public class DefaultClientInvoker<T> implements ClientInvoker {

    private Class returnType;
    private ChannelHandlerContext ctx;
    private String requestId;
    private String identity;

    private T result;

    public DefaultClientInvoker(Class returnType, ChannelHandlerContext ctx, String requestId, String identity) {
        this.returnType = returnType;
        this.ctx = ctx;
        this.requestId = requestId;
        this.identity = identity;
    }

    @Override
    public T invoke(Object[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("interfaces", identity);

        JSONObject param = new JSONObject();
        if (args != null) {
            for (Object obj : args) {
                param.put(obj.getClass().getName(), obj);
            }
        }
        jsonObject.put("parameter", param);
        jsonObject.put("requestId", requestId);
        String msg = jsonObject.toJSONString() + Constants.DELIMITER_STR;
        System.out.println("发送给服务端JSON为：" + msg);

        ByteBuf byteBuf = Unpooled.copiedBuffer(msg.getBytes());
        ctx.writeAndFlush(byteBuf);

        wait4Result();

        return result;
    }

    private void wait4Result() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setResult(String result) {
        synchronized (this) {
            this.result = (T) JSONObject.parseObject(result, returnType);
            notifyAll();
        }
    }
}
