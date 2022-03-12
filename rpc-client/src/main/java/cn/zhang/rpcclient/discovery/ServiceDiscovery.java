package cn.zhang.rpcclient.discovery;

import cn.zhang.rpcclient.client.ClientBootstrap;
import cn.zhang.rpcclient.client.ConnectHolder;
import cn.zhang.rpcclient.client.RpcClientInitializer;
import cn.zhang.rpcclient.common.MessageFuture;
import cn.zhang.rpcclient.common.RpcRequest;
import cn.zhang.rpcclient.common.RpcResponse;
import cn.zhang.rpcclient.constant.ZookeeperConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author zhang
 * @date 2022/3/10 上午9:38
 */
@Component
@Slf4j
public class ServiceDiscovery implements InitializingBean {

    @Value("${zk.service.url:null}")
    private String serviceAddr;

    private ZooKeeper zk;

    private static final ScheduledThreadPoolExecutor checkHealthExector =
            new ScheduledThreadPoolExecutor(1, r -> new Thread(r,"health-check-thread"));

    private CountDownLatch count = new CountDownLatch(1);

    protected final ConcurrentHashMap<String, MessageFuture> futures = new ConcurrentHashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
    @Autowired
    private ClientBootstrap clientBootstrap;

    private void watchNode(ZooKeeper zooKeeper) {

        try {
            List<String> nodeList = zooKeeper.getChildren(ZookeeperConstant.ZK_REGISTRY_PATH, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watchNode(zooKeeper);
                }
            });

            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(ZookeeperConstant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            updateChannels(dataList);

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateChannels(List<String> dataList) {
        ConcurrentHashMap<String, Channel> channels = ConnectHolder.getInstance().getChannels();
        if (CollectionUtils.isEmpty(dataList) && !channels.isEmpty()) {
            channels.clear();
        }
        for (String addr: dataList) {
            if (channels.isEmpty()) {
                addNewChannel(addr);
            }else {
                for (String channel: channels.keySet()) {
                    if (!addr.equals(channel)) {
                        // new server has removed
                        if (StringUtil.isNullOrEmpty(addr)) {
                            channels.remove(addr);
                        }
                        // add new server
                        if (StringUtil.isNullOrEmpty(channel)) {
                            addNewChannel(addr);
                        }
                    }
                }
            }
        }
    }

    private void addNewChannel(String addr) {
        threadPoolExecutor.submit(()->{
            Bootstrap bootstrap = new Bootstrap();
            NioEventLoopGroup eventLoopGroup  = new NioEventLoopGroup(4);
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer(futures));
            ChannelFuture connect = bootstrap.connect(toInetSocketAddress(addr));
            connect.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    ConnectHolder.getInstance().getChannels().put(addr, connect.channel());
                }
            });
        });
    }


    public static InetSocketAddress toInetSocketAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    private ZooKeeper connectServer(String serviceAddr) {
        try {
            zk = new ZooKeeper(serviceAddr, ZookeeperConstant.ZK_SESSION_TIMEOUT, new Watcher() {
                @SneakyThrows
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        count.countDown();
                    } else if (watchedEvent.getState() == Event.KeeperState.Expired) {
                        zk = new ZooKeeper(serviceAddr, ZookeeperConstant.ZK_SESSION_TIMEOUT, this);
                    }

                }
            });
            boolean await = count.await(5, TimeUnit.SECONDS);
            if (!await) {
                throw new RuntimeException("连接zk超时...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zk;
    }

    public void stop() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        zk = connectServer(serviceAddr);
        if (zk != null) {
            watchNode(zk);
            healthCheck();
        }
    }

    private void healthCheck() {
        checkHealthExector.scheduleAtFixedRate(()->{
            log.info("health-check....");
            watchNode(zk);
        }, 1, 5, TimeUnit.SECONDS);
    }

    public RpcResponse sendRequest(RpcRequest rpcRequest) throws InterruptedException, TimeoutException {
        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRpcRequest(rpcRequest);
        messageFuture.setTimeout(5000L);
        futures.put(rpcRequest.getRequestId(), messageFuture);

        Channel channel = ConnectHolder.acquireChannel("127.0.0.1:18887");

        channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                MessageFuture messageFuture1 = futures.remove(rpcRequest.getRequestId());
                if (messageFuture1 != null) {
                    messageFuture1.setResultMessage(future.cause());
                }
            }
        });

        try {
            return (RpcResponse)messageFuture.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
