package com.polaris.socket.server.iowork.impl;

import com.polaris.socket.core.socket.ISocket;
import com.polaris.socket.server.HandlerIO;
import com.polaris.socket.server.iowork.IIOManager;
import com.polaris.socket.server.iowork.IReader;
import com.polaris.socket.server.iowork.IWriter;
import com.polaris.socket.server.listener.MessageListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerIOManager implements IIOManager {

    /**
     * io写
     */
    private IWriter writer;
    /**
     * io读
     */
    private IReader reader;

    private ISocket socket;

    /**
     * idea超时时间
     */
    private long ideaTimeout = -1;

    private Timer timer;

    private final MessageListener messageListener;

    public ServerIOManager(ISocket socket) {
        this(socket, null);
    }

    public ServerIOManager(ISocket socket, MessageListener messageListener) {
        this.messageListener = messageListener;
        try {
            initIO(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //初始化io
    private void initIO(ISocket socket) throws IOException {
        writer = new ServerWriter(socket.getOutputStream(), socket); //写
        HandlerIO handlerIO = new HandlerIO(writer, messageListener);
        reader = new ServerReader(socket.getInputStream(), socket, handlerIO); //读
        this.socket = socket;
    }

    @Override
    public void sendBuffer(byte[] buffer) {
        if (writer != null) {
            writer.offer(buffer);
        }
    }

    @Override
    public void startIO() {
        if (writer != null) {
            writer.openWriter();
        }
        if (reader != null) {
            reader.openReader();
        }

        if (ideaTimeout > 0) {
            timer = new Timer();
            socket.updateIdeaTime();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // 如果时间超过idea时间
                    if ((System.currentTimeMillis() - socket.getLastIdeaTime()) > getIdeaTimeout()) {
                        log.error("流超时，上次活动时间为{}", new Date(socket.getLastIdeaTime()));
                        // 关闭流
                        closeIO();
                    }
                }
            }, 0, TimeUnit.SECONDS.toMillis(10));
        }
    }

    @Override
    public void closeIO() {
        if (writer != null) {
            writer.closeWriter();
        }
        if (reader != null) {
            reader.closeReader();
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    public long getIdeaTimeout() {
        return ideaTimeout;
    }

    public void setIdeaTimeout(long ideaTimeout) {
        this.ideaTimeout = ideaTimeout;
    }
}
