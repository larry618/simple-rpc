package com.heheda.simplerpc.rpc.protocol;


import lombok.Data;

@Data
public class RpcResponse {

    private String requestId; // response 对应的 请求id
    private Object result;  // 正常情况下的返回值
    private Throwable error;  // 异常
    private String message;  // 异常信息
}
