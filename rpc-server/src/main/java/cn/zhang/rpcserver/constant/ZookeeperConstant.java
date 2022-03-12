package cn.zhang.rpcserver.constant;

/**
 * @author zhang
 * @date 2022/3/9 下午6:18
 */
public interface ZookeeperConstant {

    int ZK_SESSION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/registry";

    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}
