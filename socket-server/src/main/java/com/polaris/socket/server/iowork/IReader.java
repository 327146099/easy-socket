package com.polaris.socket.server.iowork;

public interface IReader<T> {

    /**
     * 读数据
     */
    void read();

    /**
     * 打开数据的读取
     */
    void openReader();

    /**
     * 关闭数据的读取
     */
    void closeReader();

    /**
     * 设置参数
     * @param t
     */
    void setOption(T t);

}
