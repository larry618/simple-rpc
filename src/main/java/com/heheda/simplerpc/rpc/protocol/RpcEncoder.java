package com.heheda.simplerpc.rpc.protocol;

import com.heheda.simplerpc.common.serialize.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;


    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {

        if (genericClass.isInstance(in)) {
            byte[] bytes = JsonUtil.serialize(in);
            out.writeInt(bytes.length); // 先写一个长度 解决粘包 拆包
            out.writeBytes(bytes);
        }
    }
}
