package cn.zhang.rpcclient.controller;

import cn.zhang.rpcclient.client.RpcClient;
import cn.zhang.rpcclient.discovery.ServiceDiscovery;
import cn.zhang.rpcclient.proxy.ObjectProxy;
import cn.zhang.rpccommon.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhang
 * @date 2022/3/10 下午7:22
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private ServiceDiscovery discovery;

    @GetMapping("")
    public String getOrder() {
        OrderService orderService = RpcClient.create(OrderService.class, discovery);
        String test = orderService.getOrder("test");
        return test;
    }
}
