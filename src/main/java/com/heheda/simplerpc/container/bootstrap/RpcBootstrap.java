package com.heheda.simplerpc.container.bootstrap;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBootstrap {


    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("server-spring.xml");  // 按道理不应该由框架在做这些事情
    }
}
