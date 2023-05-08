package com.polaris.socket.core.message.impl;

import com.polaris.socket.core.message.base.SuperResponse;
import lombok.Data;

/**
 * 回调消息
 */
@Data
public class CallbackResponse extends SuperResponse {

    private String from;
}
