package com.baily.rpc.demo;

/**
 * baily
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(Student student) {
        return "hello，我收到了消息: " + student.toString();
    }
}
