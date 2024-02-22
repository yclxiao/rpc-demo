package com.baily.rpc.registry;

import java.util.List;

/**
 * baily
 */
public interface Registry {
    /**
     * 往注册中心注册服务
     *
     * @param clazz
     * @param registryInfo
     * @throws Exception
     */
    void register(Class clazz, RegistryInfo registryInfo) throws Exception;

    /**
     * 从注册中心拉取服务
     *
     * @param clazz
     * @throws Exception
     */
    List<RegistryInfo> fetchRegistry(Class clazz) throws Exception;
}
