package com.polaris.socket.core.message.impl;

import com.polaris.socket.core.message.base.SuperResponse;
import lombok.Data;

/**
 * 服务器返回的测试消息
 */
@Data
public class TestResponse extends SuperResponse {

    private String from;
}
