package com.heheda.simplerpc.rpc.proxy;


import com.heheda.simplerpc.cluster.RpcConnectionManager;
import com.heheda.simplerpc.rpc.protocol.RpcFuture;
import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import com.heheda.simplerpc.rpc.protocol.client.RpcClientHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

public class RpcServiceBeanProxy implements InvocationHandler {

    private Class<?> serviceBeanClass;

    private RpcConnectionManager rpcConnectionManager;


    public RpcServiceBeanProxy(Class<?> serviceBeanClass, RpcConnectionManager rpcConnectionManager) {
        this.serviceBeanClass = serviceBeanClass;
        this.rpcConnectionManager = rpcConnectionManager;
    }

    public RpcServiceBeanProxy(Class<?> serviceBeanClass) {
        this.serviceBeanClass = serviceBeanClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        RpcRequest request = buildRpcRequest(method, args);
        RpcClientHandler handler = rpcConnectionManager.chooseHandler(request);
        RpcFuture future = handler.sendRpcRequest(request);

        RpcResponse response = future.get();

        if (response == null) {
            return null;
        }

        if (response.getError() != null) {
            throw  response.getError();
        }

        return response.getResult();
    }


    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(serviceBeanClass.getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setParameterType(getParamsType(args));
        return request;
    }


    private Class<?>[] getParamsType(Object[] args) {
        Class[] paramsType = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramsType[i] = getClassType(args[i]);
        }
        return paramsType;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }
}
