package com.baily.rpc.demo;

import com.baily.rpc.rpc.protocol.ProxyProtocol;
import com.baily.rpc.config.ReferenceConfig;

import java.util.Collections;

/**
 * baily
 */
public class TestConsumer {
    public static void main(String[] args) throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setClazz(HelloService.class);

        String registryUrl = "zookeeper://localhost:2181";
        int port = 50073;

        ProxyProtocol proxyProtocol = new ProxyProtocol(registryUrl, null,
                Collections.singletonList(referenceConfig), port);

        HelloService helloService = proxyProtocol.getService(HelloService.class);

        System.out.println("sayHello(Student)结果为：" + helloService.sayHello(new Student("不焦躁的程序员", 31)));
        System.out.println("调用结束");


    }
}
