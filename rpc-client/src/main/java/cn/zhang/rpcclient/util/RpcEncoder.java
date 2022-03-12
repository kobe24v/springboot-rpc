package cn.zhang.rpcclient.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author zhang
 * @date 2022/3/9 下午5:47
 */
public class RpcEncoder extends MessageToByteEncoder {


    private Class<?> genericClass;

    public RpcEncoder(Class<?> clazz) {
        this.genericClass = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf out) throws Exception {
        if (genericClass.isInstance(o)) {
            byte[] data = SerializationUtil.serialize(o);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
