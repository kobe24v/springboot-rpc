package cn.zhang.rpcserver.service;


import cn.zhang.rpccommon.annotation.RpcService;
import cn.zhang.rpccommon.service.OrderService;

/**
 * @author zhang
 * @date 2022/3/10 下午7:15
 */
@RpcService(OrderService.class)
public class OrderServiceImpl implements OrderService {
    @Override
    public String getOrder(String name) {
        return "获取订单" + name;
    }
}
