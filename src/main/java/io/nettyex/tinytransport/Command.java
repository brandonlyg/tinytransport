package io.nettyex.tinytransport;

/**
 * @author brandonli
 * @created 2019-08-19
 */

import java.util.HashMap;
import java.util.Map;

/**
数据包中cmd字段的定义
 */
public enum Command {
    INVALID(0x00), //无效的cmd
    //ping pong心跳
    PING(0x01),
    PONG(0x02),

    REQUEST(0x10), //请求消息
    RESPONSE(0x12), //响应消息

    PUSH(0x20) //推送消息
    ;

    private static Map<Integer, Command> valueMap = new HashMap<>();
    static {
        valueMap.put(PING.value, PING);
        valueMap.put(PONG.value, PONG);
        valueMap.put(REQUEST.value, REQUEST);
        valueMap.put(RESPONSE.value, RESPONSE);
        valueMap.put(PUSH.value, PUSH);
    }


    private int value;

    private Command(int val){
        value = val;
    }

    public int getValue() {
        return value;
    }

    public static Command valueOf(int val){
        Command res = valueMap.get(val);
        if(null == res) res = INVALID;

        return res;
    }
}

