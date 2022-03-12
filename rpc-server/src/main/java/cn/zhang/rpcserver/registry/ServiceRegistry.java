package cn.zhang.rpcserver.registry;

import cn.zhang.rpcserver.constant.ZookeeperConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhang
 * @date 2022/3/9 下午6:00
 */
@Slf4j
@Component
public class ServiceRegistry {

    @Value("${zk.service.url:null}")
    private String serviceAddr;

    private CountDownLatch count = new CountDownLatch(1);


    public void register(String data) {
        if (data != null) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                AddRootNode(zk);// 创建node
                createNode(zk, data);
            }else {
                log.error("连接zk失败....");
            }
        }
    }

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(ZookeeperConstant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.debug("创建临时有序节点:",path,data);
            System.out.println("临时节点创建成功......");
        } catch (InterruptedException e) {
            log.error(e.toString());
        } catch (KeeperException ex) {
            log.error(ex.toString());
        }
    }

    private void AddRootNode(ZooKeeper zk) {
        try {
            Stat s = zk.exists(ZookeeperConstant.ZK_REGISTRY_PATH, false);
            if (s == null) {
                zk.create(ZookeeperConstant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (InterruptedException e) {
            log.error(e.toString());
        } catch (KeeperException ex) {
            log.error(ex.toString());
        }
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(serviceAddr, ZookeeperConstant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        count.countDown();;
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

}
