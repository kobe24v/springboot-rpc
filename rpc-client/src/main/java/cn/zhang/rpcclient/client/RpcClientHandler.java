package cn.zhang.rpcclient.client;

import cn.zhang.rpcclient.common.MessageFuture;
import cn.zhang.rpcclient.common.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhang
 * @date 2022/3/10 下午5:17
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final ConcurrentHashMap<String, MessageFuture> pendingRPC;

    public RpcClientHandler (ConcurrentHashMap<String, MessageFuture> pendingRPC) {
        this.pendingRPC = pendingRPC;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        MessageFuture messageFuture = pendingRPC.get(rpcResponse.getRequestId());
        messageFuture.setResultMessage(rpcResponse);
    }



}
