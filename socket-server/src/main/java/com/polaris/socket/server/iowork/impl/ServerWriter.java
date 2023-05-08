package com.polaris.socket.server.iowork.impl;

import com.polaris.socket.core.socket.ISocket;
import com.polaris.socket.server.iowork.IWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;

public class ServerWriter implements IWriter {

    /**
     * 输出流
     */
    private final OutputStream outputStream;

    /**
     * 写入数据的线程
     */
    private Thread writerThread;
    /**
     * 需要写入的数据
     */
    private final LinkedBlockingDeque<byte[]> packetsToSend = new LinkedBlockingDeque<>();
    /**
     * 是否关闭线程
     */
    private volatile boolean isShutdown;

    private final ISocket socket;

    public ServerWriter(OutputStream outputStream, ISocket socket) {
        this.outputStream = outputStream;
        this.socket = socket;
    }

    @Override
    public void openWriter() {
        writerThread = new Thread(writerTask, "writer thread");
        isShutdown = false;
        writerThread.start();
    }

    @Override
    public void setOption(Object t) {

    }

    /**
     * io写任务
     */
    private final Runnable writerTask = new Runnable() {
        @Override
        public void run() {
            //只要socket处于连接的状态，就一直活动
            while (socket.isConnected() && !isShutdown && !socket.isClosed()) {
                try {
                    byte[] sender = packetsToSend.take();
                    write(sender);
                } catch (InterruptedException e) {
                    //取数据异常
                    e.printStackTrace();
                    isShutdown = true;
                }
            }
        }
    };

    @Override
    public void write(byte[] sendBytes) {
        if (sendBytes != null) {
            try {
                int packageSize = 100; //每次发送的数据包的大小
                int remainingCount = sendBytes.length;
                ByteBuffer writeBuf = ByteBuffer.allocate(packageSize); //分配一个内存缓存
                writeBuf.order(ByteOrder.BIG_ENDIAN);
                int index = 0;
                //如果要发送的数据大小大于每次发送的数据包的大小， 则要分多次将数据发出去
                while (remainingCount > 0) {
                    int realWriteLength = Math.min(packageSize, remainingCount);
                    writeBuf.clear(); //清空缓存
                    writeBuf.rewind(); //将position位置移到0
                    writeBuf.put(sendBytes, index, realWriteLength);
                    writeBuf.flip(); //将position赋为0，limit赋为数据大小
                    byte[] writeArr = new byte[realWriteLength];
                    writeBuf.get(writeArr);
                    outputStream.write(writeArr);
                    outputStream.flush(); //强制缓存中残留的数据写入清空
                    index += realWriteLength;
                    remainingCount -= realWriteLength;
                }
                this.socket.updateIdeaTime();
            } catch (Exception e) {
                //写数据异常
                e.printStackTrace();
                isShutdown = true;
            }
        }
    }

    @Override
    public void offer(byte[] sender) {
        packetsToSend.offer(sender);
    }

    @Override
    public void closeWriter() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            shutDownThread();
        } catch (IOException ignored) {
        }
    }

    private void shutDownThread() {
        isShutdown = true;
        if (writerThread != null && writerThread.isAlive() && !writerThread.isInterrupted()) {
            writerThread.interrupt();
        }
    }
}
