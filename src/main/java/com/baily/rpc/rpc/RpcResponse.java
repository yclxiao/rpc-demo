package com.baily.rpc.rpc;

import lombok.Data;
import lombok.ToString;

/**
 * baily
 */
@Data
@ToString
public class RpcResponse {
    private String result;
    private String interfaceIdentity;
    private String requestId;

    public static RpcResponse create(String result, String interfaceIdentity, String requestId) {

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setInterfaceIdentity(interfaceIdentity);
        rpcResponse.setRequestId(requestId);
        rpcResponse.setResult(result);

        return rpcResponse;
    }
}
