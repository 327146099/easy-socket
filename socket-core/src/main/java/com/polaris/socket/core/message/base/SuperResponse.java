package com.polaris.socket.core.message.base;

import lombok.Data;

@Data
public class SuperResponse implements IResponse {

    /**
     * 消息ID
     */
    private String msgId;

    private String callbackId;

    private String body;

}
