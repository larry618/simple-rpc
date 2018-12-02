package com.heheda.simplerpc.rpc.protocol.server;


import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 服务端的 netty channelHandler
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private Map<String, Object> handlers;

    private volatile ThreadPoolExecutor threadPoolExecutor;

    private Map<String, Method> methodCache = new ConcurrentHashMap<>();

    public RpcServerHandler(Map<String, Object> handlers) {
        this.handlers = handlers;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {


        logger.info("received request: ", rpcRequest.toString());
        // 提交到线程池异步执行
        submit(new Runnable() {

            @Override
            public void run() {
                RpcResponse rpcResponse = handler(rpcRequest);

                ctx.writeAndFlush(rpcResponse).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.debug("Send response for request " + rpcRequest);
                    }
                });
            }
        });

    }

    private RpcResponse handler(RpcRequest request) {

        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            String className = request.getClassName();
            Object serviceBean = handlers.get(className);
            Method method = findMethod(request);
            Object result = method.invoke(serviceBean, request.getParameters());
            response.setResult(result);
        } catch (Throwable e) {
            response.setError(e);
            response.setMessage(e.getMessage());
        }

        return response;
    }

    private Method findMethod(RpcRequest request) throws NoSuchMethodException {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        String key = className + "." + methodName;
        Method cachedMethod = methodCache.get(key);

        if (cachedMethod != null) {
            return cachedMethod;
        }
        Class<?> serviceClass = handlers.get(className).getClass();

        Method method = serviceClass.getMethod(methodName, request.getParameterType());
        method.setAccessible(true);
        methodCache.put(key, method);
        return method;
    }


    private void submit(Runnable runnable) {
        if (threadPoolExecutor == null) {
            synchronized (RpcServerHandler.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65535),
                            new ThreadPoolExecutor.CallerRunsPolicy());
                }
            }
        }
        threadPoolExecutor.submit(runnable);
    }
}
