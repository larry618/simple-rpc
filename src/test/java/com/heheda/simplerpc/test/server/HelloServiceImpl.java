package com.heheda.simplerpc.test.server;

import com.heheda.simplerpc.config.annotation.RpcService;
import com.heheda.simplerpc.test.client.HelloService;
import com.heheda.simplerpc.test.client.Person;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl() {

    }

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
