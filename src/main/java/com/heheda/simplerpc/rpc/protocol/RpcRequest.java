package com.heheda.simplerpc.rpc.protocol;

import lombok.Data;

@Data
public class RpcRequest {

    private String requestId;  // 请求id
    private String className;  // 被调用方接口名
    private String methodName; // 调用的参数
    private Class[] parameterType;  // 方法参数类型 (支持重载)
    private Object[] parameters;   // 实际的参数
}
