package com.heheda.simplerpc.test.app;

import com.heheda.simplerpc.rpc.protocol.client.RpcClient;
import com.heheda.simplerpc.test.client.HelloService;
import com.heheda.simplerpc.test.client.Person;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class ServiceTest {

    @Autowired
    private RpcClient rpcClient;

    @Test
    public void helloTest1() {

        HelloService helloService = rpcClient.createServiceBeanProxy(HelloService.class);
        String result = helloService.hello("World");
        Assert.assertEquals("Hello! World", result);
    }

    @Test
    public void helloTest2() {
        HelloService helloService = rpcClient.createServiceBeanProxy(HelloService.class);
        Person person = new Person("Yong", "Huang");
        String result = helloService.hello(person);
        Assert.assertEquals("Hello! Yong Huang", result);
    }

    @After
    public void setTear() {
        if (rpcClient != null) {
            rpcClient.stop();
        }
    }

}
