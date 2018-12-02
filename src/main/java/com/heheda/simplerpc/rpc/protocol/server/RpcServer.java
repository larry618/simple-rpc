package com.heheda.simplerpc.rpc.protocol.server;


import com.heheda.simplerpc.config.annotation.RpcService;
import com.heheda.simplerpc.registry.ServiceRegistry;
import com.heheda.simplerpc.rpc.protocol.RpcDecoder;
import com.heheda.simplerpc.rpc.protocol.RpcEncoder;
import com.heheda.simplerpc.rpc.protocol.RpcRequest;
import com.heheda.simplerpc.rpc.protocol.RpcResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    @Value("${server.address}")
    private String serverAddress;

    private Map<String, Object> handlerMap = new HashMap<>();

    @Autowired
    private ServiceRegistry serviceRegistry;

    private NioEventLoopGroup bossGroup = null;
    private NioEventLoopGroup workerGroup = null;


    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(RpcService.class);
        for (Object bean : beans.values()) {
            String interfaceName = bean.getClass().getAnnotation(RpcService.class).value().getName();
            addService(interfaceName, bean);
        }
    }


    public RpcServer addService(String interfaceName, Object serviceBean) {
        if (!handlerMap.containsKey(interfaceName)) {
            logger.info("Loading service: {}", interfaceName);
            handlerMap.put(interfaceName, serviceBean);
        }

        return this;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }


    public void start() throws Exception {

        if (bossGroup != null || workerGroup != null) {
            return;
        }

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

        serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                        .addLast(new RpcDecoder(RpcRequest.class))
                        .addLast(new RpcEncoder(RpcResponse.class))
                        .addLast(new RpcServerHandler(handlerMap));
            }
        });


        String[] arr = serverAddress.split(":");

        ChannelFuture channelFuture = serverBootstrap.bind(arr[0], Integer.parseInt(arr[1])).sync();
        logger.info("Server started on " + serverAddress);
        export();
        channelFuture.channel().closeFuture().sync();
    }


    private void export() {
        if (serverAddress == null) {
            return;
        }

//        for (String serviceName :handlerMap.keySet() ) {
//            String data = serviceName + "@" + serverAddress;
//            serviceRegistry.register(data);
//        }

        serviceRegistry.register(serverAddress);

    }


    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

}
