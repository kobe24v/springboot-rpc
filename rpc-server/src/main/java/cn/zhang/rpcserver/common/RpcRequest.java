package cn.zhang.rpcserver.common;

import lombok.Data;

/**
 * Rpc Request 发送请求的对象
 *
 * @author 沈文兵
 */
@Data
public class RpcRequest {

    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
