package com.baily.rpc.demo;

import com.baily.rpc.rpc.protocol.ProxyProtocol;
import com.baily.rpc.config.ServiceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * baily
 */
public class TestProducer {
    public static void main(String[] args) throws Exception {
        String registryUrl = "zookeeper://localhost:2181";
        int port = 50073;

        HelloService helloService = new HelloServiceImpl();
        ServiceConfig serviceConfig = new ServiceConfig(HelloService.class, helloService);
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
        serviceConfigList.add(serviceConfig);

        ProxyProtocol proxyProtocol = new ProxyProtocol(registryUrl, serviceConfigList, null, port);

    }
}
