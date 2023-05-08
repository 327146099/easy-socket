package com.polaris.socket.server.listener;

import com.polaris.socket.core.message.base.SuperClient;

public interface MessageListener {
    String receiveMessage(SuperClient clientMsg);
}
