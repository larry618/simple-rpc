package com.heheda.simplerpc.rpc.protocol.client;

import com.heheda.simplerpc.cluster.RpcConnectionManager;
import com.heheda.simplerpc.rpc.proxy.RpcServiceBeanProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Proxy;

@Service
public class RpcClient {

    @Autowired
    private RpcConnectionManager rpcConnectionManager;

    // 后续改为 ProxyFactory 模式
    @SuppressWarnings("unchecked")
    public <T> T createServiceBeanProxy(Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class[] {serviceInterface},
                new RpcServiceBeanProxy(serviceInterface, rpcConnectionManager)
        );
    }


    public void stop() {
        rpcConnectionManager.stop();
    }
}
