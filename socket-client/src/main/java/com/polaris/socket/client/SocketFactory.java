package com.polaris.socket.client;

import com.polaris.socket.core.socket.ISocket;

public interface SocketFactory {
    ISocket build();

}
