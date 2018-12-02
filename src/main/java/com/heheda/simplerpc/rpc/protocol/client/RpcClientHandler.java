package com.heheda.simplerpc.rpc.protocol.client;

import com.heheda.simplerpc.rpc.protocol.RpcFuture;
import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Map<String, RpcFuture> pendingRequest = new ConcurrentHashMap<>();

    private Channel channel;
    private SocketAddress remotePeer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        remotePeer = ctx.channel().remoteAddress();
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
