package cn.zhang.rpcclient.util;

import java.util.Map;

/**
 * @author zhang
 * @date 2022/3/9 下午5:44
 */
public class MapUtils {


    public static boolean isNotEmpty(Map map) {
        return map == null || map.isEmpty();
    }
}
