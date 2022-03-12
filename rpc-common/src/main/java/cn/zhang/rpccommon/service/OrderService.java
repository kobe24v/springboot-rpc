package cn.zhang.rpccommon.service;

import cn.zhang.rpccommon.annotation.RpcService;

/**
 * @author zhang
 * @date 2022/3/10 下午7:07
 */
@RpcService(OrderService.class)
public interface OrderService {

    String getOrder(String name);
}
