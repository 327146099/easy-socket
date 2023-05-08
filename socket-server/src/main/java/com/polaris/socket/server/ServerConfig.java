package com.polaris.socket.server;


import com.polaris.socket.core.protocol.IMessageProtocol;

public class ServerConfig {

    // 消息协议
    private IMessageProtocol messageProtocol;
    // 单例
    private static ServerConfig instance = new ServerConfig();

    public static ServerConfig getInstance() {
        return instance;
    }

    private ServerConfig() {
    }

    public IMessageProtocol getMessageProtocol() {
        return messageProtocol;
    }

    public void setMessageProtocol(IMessageProtocol messageProtocol) {
        this.messageProtocol = messageProtocol;
    }

}
