package com.baily.rpc.cluster;

import com.baily.rpc.registry.RegistryInfo;

import java.util.List;

/**
 * baily
 */
public interface LoadBalancer {
    /**
     * 根据负载均衡规则，选择某个注册机器
     *
     * @param registryInfoList
     * @return
     * @throws Exception
     */
    RegistryInfo choose(List<RegistryInfo> registryInfoList) throws Exception;
}
