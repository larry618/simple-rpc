package com.heheda.simplerpc.rpc.protocol.client;

import com.heheda.simplerpc.rpc.protocol.RpcFuture;
import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    private Map<String, RpcFuture> pendingRequest = new ConcurrentHashMap<>();

    private Channel channel;
    private SocketAddress remotePeer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        channel = ctx.channel();
        logger.info("channelRegistered : ", channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        remotePeer = ctx.channel().remoteAddress();
        logger.info("channelActive : ==>", remotePeer);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        // 接受到消息
        String requestId = response.getRequestId();
        RpcFuture future = pendingRequest.get(requestId);
        future.done(response);
    }


    public RpcFuture sendRpcRequest(RpcRequest request) {
        RpcFuture future = new RpcFuture(request);
        channel.writeAndFlush(request);
        pendingRequest.put(request.getRequestId(), future);
        return future;


//        final CountDownLatch latch = new CountDownLatch(1);
//        RpcFuture rpcFuture = new RpcFuture(request);
//        pendingRequest.put(request.getRequestId(), rpcFuture);
//        channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) {
//                latch.countDown();
//            }
//        });
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            logger.error(e.getMessage());
//        }
//
//        return rpcFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }


    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
