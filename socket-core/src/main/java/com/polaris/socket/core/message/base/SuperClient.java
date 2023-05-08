package com.polaris.socket.core.message.base;

import lombok.Data;

@Data
public class SuperClient implements IClient {

    /**
     * 消息ID
     */
    private String msgId;
    /**
     * 回调标识
     */
    private String callbackId;

    private String body;
}
