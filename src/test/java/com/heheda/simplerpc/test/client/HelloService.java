package com.heheda.simplerpc.test.client;

public interface HelloService {
    String hello(String name);

    String hello(Person person);
}
