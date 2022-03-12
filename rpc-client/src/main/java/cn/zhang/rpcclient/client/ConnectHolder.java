package cn.zhang.rpcclient.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhang
 * @date 2022/3/10 下午4:38
 */
@Slf4j
public class ConnectHolder {

    private static ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    private volatile static ConnectHolder connectHolder;


    private ConnectHolder() {}

    public static ConnectHolder getInstance() {
        if (connectHolder == null) {
            synchronized (ConnectHolder.class) {
                if (connectHolder == null) {
                    connectHolder = new ConnectHolder();
                }
            }
        }
        return connectHolder;
    }


    public static Channel acquireChannel(String addr) {
        Channel channel = channels.get(addr);
        if (channel != null) {
            return channel;
        } else {
            channels.remove(addr);
            log.error("没有获取到channel..");
            return null;
        }
    }


    public void stop() {
        for (Channel channel: channels.values()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
//        threadPoolExecutor.shutdown();
    }

    public ConcurrentHashMap<String, Channel> getChannels() {
        return channels;
    }
}
