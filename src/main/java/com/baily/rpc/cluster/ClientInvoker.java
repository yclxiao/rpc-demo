package com.baily.rpc.cluster;

/**
 * baily
 */
public interface ClientInvoker<T> {

    T invoke(Object[] args);

    void setResult(String result);
}
