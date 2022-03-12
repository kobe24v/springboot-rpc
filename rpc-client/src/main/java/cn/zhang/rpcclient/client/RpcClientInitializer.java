package cn.zhang.rpcclient.client;

import cn.zhang.rpcclient.common.MessageFuture;
import cn.zhang.rpcclient.common.RpcRequest;
import cn.zhang.rpcclient.common.RpcResponse;
import cn.zhang.rpcclient.util.RpcDecoder;
import cn.zhang.rpcclient.util.RpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhang
 * @date 2022/3/10 下午5:15
 */
public class RpcClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private ConcurrentHashMap<String, MessageFuture> futures;

    public RpcClientInitializer (ConcurrentHashMap<String, MessageFuture> pendingRPC) {
        this.futures = pendingRPC;
    }

    @Override
    protected void initChannel(NioSocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();
        System.out.println("rpc客户端初始化......");
        cp.addLast(new RpcEncoder(RpcRequest.class));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class));
        cp.addLast(new RpcClientHandler(futures));
    }
}
