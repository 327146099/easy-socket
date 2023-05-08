package com.polaris.socket.server.iowork;

public interface IIOManager {

    /**
     * 发送字节序列的数据
     * @param buffer
     */
    void sendBuffer(byte[] buffer);

    /**
     * 关闭io管理器
     */
    void closeIO();

    /**
     * 开启io操作
     */
    void startIO();

}
