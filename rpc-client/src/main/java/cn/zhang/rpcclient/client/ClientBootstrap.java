package cn.zhang.rpcclient.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @author zhang
 * @date 2022/3/10 下午5:11
 */
@Component
public class ClientBootstrap{

    private Bootstrap bootstrap;

    private EventLoopGroup eventLoopGroup;

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
