package com.baily.rpc.rpc;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * baily
 */
@Data
public class RpcRequest {
    /**
     * netty channel
     */
    private ChannelHandlerContext ctx;
    /**
     * 每次调用产生的id
     */
    private String requestId;
    /**
     * class+method组合的key
     */
    private String interfaceIdentity;
    /**
     * 方法参数类型 和 参数值，对应的 k -> v
     * 方法参数对象，对应的属性，和属性值
     */
    private Map<String, Object> parameterMap;

    public static RpcRequest parse(String message, ChannelHandlerContext ctx) throws ClassNotFoundException {
        JSONObject requestObj = JSONObject.parseObject(message);
        String interfaces = requestObj.getString("interfaces");
        JSONObject parameter = requestObj.getJSONObject("parameter");
        String requestId = requestObj.getString("requestId");

        Map<String, Object> parameterMap = new HashMap<>();
        Set<String> parameterKeySet = parameter.keySet();

        for (String parameterKey : parameterKeySet) {
            if (parameterKey.equals("java.lang.String")) {
                parameterMap.put(parameterKey, parameter.getString(parameterKey));
            } else {
                Class clazz = Class.forName(parameterKey);
                Object object = parameter.getObject(parameterKey, clazz);
                parameterMap.put(parameterKey, object);
            }
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setCtx(ctx);
        rpcRequest.setInterfaceIdentity(interfaces);
        rpcRequest.setRequestId(requestId);
        rpcRequest.setParameterMap(parameterMap);

        return rpcRequest;
    }
}
