package com.heheda.simplerpc.cluster;

import com.heheda.simplerpc.rpc.protocol.RpcDecoder;
import com.heheda.simplerpc.rpc.protocol.RpcEncoder;
import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import com.heheda.simplerpc.rpc.protocol.client.RpcClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 1. 保存连接
 * 2. 更新连接
 * 3. 选择连接
 */
@Service
public class RpcConnectionManager {

    private Map<SocketAddress, RpcClientHandler> remoteAddressMap = new ConcurrentHashMap<>();

    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();  // 用于后台异步链接 server

    private NioEventLoopGroup clientGroup = new NioEventLoopGroup();

    public RpcClientHandler chooseHandler(RpcRequest request) {
        return loadBalance(connectedHandlers);
    }

    public RpcClientHandler loadBalance(List<RpcClientHandler> handlers) {
        return handlers.get(0);
    }


    /**
     *
     */
    public void updateConnectedServer(List<String> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            closeAll();
        }

        // new list
        Set<SocketAddress> newServerAddress = new HashSet<>();
        for (String address : dataList) {
            String[] arr = StringUtils.split(address, ':');
            InetSocketAddress socketAddress = new InetSocketAddress(arr[0], Integer.parseInt(arr[1]));
            newServerAddress.add(socketAddress);
        }

        // remove invalid
        for (SocketAddress oldAddress : remoteAddressMap.keySet()) {
            if (!newServerAddress.contains(oldAddress)) { // 新节点里不包含
                closeConnection(oldAddress);
            }
        }

        // add new server
        for (SocketAddress newAddress : newServerAddress) {
            if (!remoteAddressMap.containsKey(newAddress)) {
//                connect(newAddress);
                asyncConnect(newAddress);
            }
        }

    }

    private void closeConnection(SocketAddress address) {
        RpcClientHandler handler = remoteAddressMap.get(address);
        handler.close();
        remoteAddressMap.remove(address);
        connectedHandlers.remove(handler);
    }


    private void asyncConnect(SocketAddress address) {
        executorService.submit(() -> connect(address));
    }

    private void connect(SocketAddress address) {
        Bootstrap bootstrap = new Bootstrap();
        RpcClientHandler rpcClientHandler = new RpcClientHandler();
        bootstrap.group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {

                        socketChannel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                .addLast(new RpcDecoder(RpcRequest.class))
                                .addLast(new RpcEncoder(RpcResponse.class))
                                .addLast(rpcClientHandler);
                    }
                });
        ChannelFuture future = bootstrap.connect(address);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                addHandler(rpcClientHandler);
            }
        });
    }


    private void addHandler(RpcClientHandler rpcClientHandler) {
        SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
        remoteAddressMap.put(remotePeer, rpcClientHandler);
        connectedHandlers.add(rpcClientHandler);
    }

    private void closeAll() {
        for (RpcClientHandler handler : connectedHandlers) {
            handler.close();
        }
        remoteAddressMap.clear();
        connectedHandlers.clear();
    }

}


