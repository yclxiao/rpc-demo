package com.baily.rpc.registry;

import com.alibaba.fastjson.JSONArray;
import com.baily.rpc.common.InvokeUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * baily
 */
public class ZkRegistry implements Registry {

    private String registryUrl;
    private CuratorFramework client;

    private static String rootPath = "/rpc-demo";


    /**
     * 初始化：创建客户端，创建目录
     *
     * @param registryUrl
     */
    public ZkRegistry(String registryUrl) {
        this.registryUrl = registryUrl;

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(this.registryUrl, retryPolicy);
        client.start();
        try {
            Stat stat = client.checkExists().forPath(rootPath);
            if (stat == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .forPath(rootPath);
            }
            System.out.println("ZK Registry 初始化完毕");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接口的方法，以某种形式组织起来，作为文件节点，对应的机器信息作为内容，写到文件节点里
     * 写的zk node的时候，是按照 class + method组织key的，所以，需要记录  key -> method的关系
     *
     * @param clazz
     * @param registryInfo
     */
    @Override
    public void register(Class clazz, RegistryInfo registryInfo) throws Exception {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method tempMethod : methods) {
            String key = InvokeUtils.buildInterfaceMethodIdentify(clazz, tempMethod);
            String path = rootPath + "/" + key;
            Stat stat = client.checkExists().forPath(path);

            List<RegistryInfo> registryInfoList = new ArrayList<>();
            if (stat != null) {
                byte[] alreadyRegistryInfoBytes = client.getData().forPath(path);
                String alreadyRegistryInfoStr = new String(alreadyRegistryInfoBytes, StandardCharsets.UTF_8);
                registryInfoList = JSONArray.parseArray(alreadyRegistryInfoStr, RegistryInfo.class);
                if (registryInfoList.contains(registryInfo)) {
                    System.out.println("地址列表中已经包含了【" + key + "】，无需注册了");
                } else {
                    registryInfoList.add(registryInfo);
                    client.setData().forPath(path, JSONArray.toJSONString(registryInfoList).getBytes());
                    System.out.println("注册到注册中心，路径为：【" + path + "】信息为：" + registryInfo);
                }
            } else {
                registryInfoList.add(registryInfo);
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path, JSONArray.toJSONString(registryInfoList).getBytes());
                System.out.println("注册到注册中心，路径为：【" + path + "】信息为：" + registryInfo);
            }

        }
    }

    @Override
    public List<RegistryInfo> fetchRegistry(Class clazz) throws Exception {
        List<RegistryInfo> registryInfoList = null;

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String key = InvokeUtils.buildInterfaceMethodIdentify(clazz, method);
            String path = rootPath + "/" + key;
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {
                // 这里可以添加watcher来监听变化，这里简化了，没有做这个事情
                System.out.println("警告：无法找到服务接口：" + path);
                continue;
            }
            if (registryInfoList == null) {
                byte[] bytes = client.getData().forPath(path);
                String data = new String(bytes, StandardCharsets.UTF_8);
                registryInfoList = JSONArray.parseArray(data, RegistryInfo.class);
            }
        }

        return registryInfoList;
    }
}
