package com.baily.rpc.rpc.protocol;

import com.alibaba.fastjson.JSONObject;
import com.baily.rpc.cluster.ClientInvoker;
import com.baily.rpc.cluster.DefaultClientInvoker;
import com.baily.rpc.cluster.LoadBalancer;
import com.baily.rpc.cluster.RandomLoadBalancer;
import com.baily.rpc.config.ReferenceConfig;
import com.baily.rpc.config.ServiceConfig;
import com.baily.rpc.registry.Registry;
import com.baily.rpc.registry.RegistryInfo;
import com.baily.rpc.registry.ZkRegistry;
import com.baily.rpc.remote.NettyClient;
import com.baily.rpc.remote.NettyServer;
import com.baily.rpc.rpc.RpcResponse;
import com.baily.rpc.common.InvokeUtils;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * baily
 */
public class ProxyProtocol {

    private String registryUrl;
    private List<ServiceConfig> serviceConfigList;
    private int port;

    private Registry registry;
    private NettyServer nettyServer;
    private RegistryInfo registryInfo;

    public static Map<String, Method> interfaceMethodMap = new ConcurrentHashMap<>();

    private List<ReferenceConfig> referenceConfigList;
    private ResponseProcessor[] responseProcessors;

    private ConcurrentLinkedQueue<RpcResponse> messageQueue = new ConcurrentLinkedQueue<>();
    //处理中的invoker，接口信息对应的invoker
    public static Map<String, ClientInvoker> inProcessInvokerMap = new ConcurrentHashMap<>();
    //接口对应的，注册信息列表
    public static Map<Class, List<RegistryInfo>> interfaceMethodsRegistryInfoMap = new ConcurrentHashMap<>();
    //注册中心对应的channelcontext
    public static Map<RegistryInfo, ChannelHandlerContext> registryChannelMap = new ConcurrentHashMap<>();

    private LongAdder requestIdWorker = new LongAdder();

    public ProxyProtocol(String registryUrl, List<ServiceConfig> serviceConfigList, List<ReferenceConfig> referenceConfigList, int port) throws Exception {
        this.serviceConfigList = serviceConfigList == null ? new ArrayList<>() : serviceConfigList;
        this.registryUrl = registryUrl;
        this.port = port;
        this.referenceConfigList = referenceConfigList == null ? new ArrayList<>() : referenceConfigList;

        //1、初始化注册中心
        initRegistry(this.registryUrl);

        //2、将服务注册到注册中心
        InetAddress addr = InetAddress.getLocalHost();
        String hostName = addr.getHostName();
        String hostAddr = addr.getHostAddress();
        registryInfo = new RegistryInfo(hostName, hostAddr, this.port);
        doRegistry(registryInfo);

        //3、初始化nettyServer，启动nettyServer
        if (!this.serviceConfigList.isEmpty()) {
            nettyServer = new NettyServer(this.serviceConfigList, this.interfaceMethodMap);
            nettyServer.init(this.port);
        }

        //如果是客户端引用启动，则初始化处理线程
        if (!this.referenceConfigList.isEmpty()) {
            initProcessor();
        }
    }

    /**
     * 初始化注册中心
     * 创建跟目录等等
     * 暂时只使用ZK
     *
     * @param registryUrl
     */
    private void initRegistry(String registryUrl) {
        if (registryUrl.startsWith("zookeeper://")) {
            registryUrl = registryUrl.substring(12);
            registry = new ZkRegistry(registryUrl);
        }
    }

    /**
     * 将服务注册到注册中心
     * 将ServiceConfig的类和方法，以某种形式组织成key，写入到ZK
     * 写的zk node的时候，是按照 class + method组织key的，所以，需要记录  key -> method的关系
     *
     * @param registryInfo
     */
    private void doRegistry(RegistryInfo registryInfo) throws Exception {
        for (ServiceConfig serviceConfig : this.serviceConfigList) {
            Class type = serviceConfig.getType();
            registry.register(type, registryInfo);
            Method[] methods = type.getDeclaredMethods();
            for (Method tempMethod : methods) {
                String key = InvokeUtils.buildInterfaceMethodIdentify(type, tempMethod);
                interfaceMethodMap.put(key, tempMethod);
            }
        }

        for (ReferenceConfig referenceConfig : this.referenceConfigList) {
            List<RegistryInfo> registryInfoList = registry.fetchRegistry(referenceConfig.getClazz());
            if (registryInfoList != null) {
                interfaceMethodsRegistryInfoMap.put(referenceConfig.getClazz(), registryInfoList);
                initChannel(registryInfoList);
            }
        }
    }

    /**
     * 获取服务，初始化通信channel
     */
    private void initChannel(List<RegistryInfo> registryInfoList) throws InterruptedException {
        for (RegistryInfo registryInfo : registryInfoList) {
            if (!registryChannelMap.containsKey(registryInfo)) {
                NettyClient nettyClient = new NettyClient(registryInfo.getIp(), registryInfo.getPort());
                nettyClient.setMessageCallback(message -> {
                    RpcResponse rpcResponse = JSONObject.parseObject(message, RpcResponse.class);
                    messageQueue.offer(rpcResponse);
                    synchronized (ProxyProtocol.this) {
                        ProxyProtocol.this.notifyAll();
                    }
                });
                //初始化链接之后，需要等待链接成功，在链接成功之前，堵塞
                ChannelHandlerContext ctx = nettyClient.getCtx();
                registryChannelMap.put(registryInfo, ctx);
            }
        }
    }

    /**
     * 获取代理Service
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class clazz) throws Exception {

        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("equals".equals(methodName) || "hashCode".equals(methodName)) {
                    throw new IllegalAccessException("不能访问" + methodName + "方法");
                }
                if ("toString".equals(methodName)) {
                    return clazz.getName() + "#" + methodName;
                }

                List<RegistryInfo> registryInfoList = interfaceMethodsRegistryInfoMap.get(clazz);
                if (registryInfoList == null) {
                    throw new RuntimeException("无法找到对应的服务提供者");
                }

                LoadBalancer loadBalancer = new RandomLoadBalancer();
                RegistryInfo registryInfo = loadBalancer.choose(registryInfoList);

                ChannelHandlerContext ctx = registryChannelMap.get(registryInfo);

                String identity = InvokeUtils.buildInterfaceMethodIdentify(clazz, method);
                String requestId;

                synchronized (ProxyProtocol.this) {
                    requestIdWorker.increment();
                    requestId = String.valueOf(requestIdWorker.longValue());
                }

                ClientInvoker clientInvoker = new DefaultClientInvoker(method.getReturnType(), ctx, requestId, identity);

                inProcessInvokerMap.put(identity + "#" + requestId, clientInvoker);

                return clientInvoker.invoke(args);
            }
        });
    }

    /**
     * 初始化客户端的处理线程 processor
     */
    private void    initProcessor() {
        //启动几个客户端处理processor
        int num = 3;
        responseProcessors = new ResponseProcessor[num];
        for (int i = 0; i < num; i++) {
            responseProcessors[i] = new ResponseProcessor("clientProcessorThread-" + i);
            responseProcessors[i].start();
        }
    }

    private class ResponseProcessor extends Thread {
        public ResponseProcessor(String s) {
            super(s);
        }

        @Override
        public void run() {
            System.out.println("启动响应处理线程：" + getName());
            while (true) {
                RpcResponse rpcResponse = messageQueue.poll();
                if (rpcResponse == null) {
                    try {
                        synchronized (ProxyProtocol.this) {
                            ProxyProtocol.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("收到一个响应：" + rpcResponse);

                    String identity = rpcResponse.getInterfaceIdentity();
                    String requestId = rpcResponse.getRequestId();
                    String result = rpcResponse.getResult();

                    String key = identity + "#" + requestId;
                    ClientInvoker clientInvoker = inProcessInvokerMap.remove(key);
                    clientInvoker.setResult(result);
                }
            }
        }
    }

}
