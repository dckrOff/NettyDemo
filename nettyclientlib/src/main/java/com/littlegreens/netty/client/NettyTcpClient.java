package com.littlegreens.netty.client;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.littlegreens.netty.client.handler.NettyClientHandler;
import com.littlegreens.netty.client.listener.MessageStateListener;
import com.littlegreens.netty.client.listener.NettyClientListener;
import com.littlegreens.netty.client.status.ConnectState;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

/**
 * Created by littleGreens on 2018-11-10.
 * TCP client
 */
public class NettyTcpClient {
    private static final String TAG = "NettyTcpClient";

    private EventLoopGroup group;

    private NettyClientListener listener;

    private Channel channel;

    private boolean isConnect = false;

    /**
     * Maximum number of reconnections
     */
    private int MAX_CONNECT_TIMES = Integer.MAX_VALUE;

    private int reconnectNum = MAX_CONNECT_TIMES;

    private boolean isNeedReconnect = true;
    private boolean isConnecting = false;

    private long reconnectIntervalTime = 5000;
    private static final Integer CONNECT_TIMEOUT_MILLIS = 5000;

    private String host;
    private int tcp_port;
    private int mIndex;
    /**
     * heartbeat interval
     */
    private long heartBeatInterval = 5;//单位秒

    /**
     * Whether to send heartbeat
     */
    private boolean isSendheartBeat = false;

    /**
     * Heartbeat data, which can be String type or byte[].
     */
    private Object heartBeatData;

    private String packetSeparator;
    private int maxPacketLong = 1024;

    private void setPacketSeparator(String separator) {
        this.packetSeparator = separator;
    }

    private void setMaxPacketLong(int maxPacketLong) {
        this.maxPacketLong = maxPacketLong;
    }

    private NettyTcpClient(String host, int tcp_port, int index) {
        this.host = host;
        this.tcp_port = tcp_port;
        this.mIndex = index;
    }

    public int getMaxConnectTimes() {
        return MAX_CONNECT_TIMES;
    }

    public long getReconnectIntervalTime() {
        return reconnectIntervalTime;
    }

    public String getHost() {
        return host;
    }

    public int getTcp_port() {
        return tcp_port;
    }

    public int getIndex() {
        return mIndex;
    }

    public long getHeartBeatInterval() {
        return heartBeatInterval;
    }

    public boolean isSendheartBeat() {
        return isSendheartBeat;
    }

    public void connect() {
        if (isConnecting) {
            return;
        }
        Thread clientThread = new Thread("client-Netty") {
            @Override
            public void run() {
                super.run();
                isNeedReconnect = true;
                reconnectNum = MAX_CONNECT_TIMES;
                connectServer();
            }
        };
        clientThread.start();
    }


    private void connectServer() {
        synchronized (NettyTcpClient.this) {
            ChannelFuture channelFuture = null;
            if (!isConnect) {
                isConnecting = true;
                group = new NioEventLoopGroup();
                Bootstrap bootstrap = new Bootstrap().group(group)
                        .option(ChannelOption.TCP_NODELAY, true) // Shield Nagle's algorithm from trying to
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                if (isSendheartBeat) {
                                    ch.pipeline().addLast("ping", new IdleStateHandler(0, heartBeatInterval, 0, TimeUnit.SECONDS)); // 5s no data sent, callback userEventTriggered
                                }

                                // Sticky package processing requires the cooperation of client and server

                                if (!TextUtils.isEmpty(packetSeparator)) {
                                    ByteBuf delimiter = Unpooled.buffer();
                                    delimiter.writeBytes(packetSeparator.getBytes());
                                    ch.pipeline().addLast(new DelimiterBasedFrameDecoder(maxPacketLong, delimiter));
                                } else {
                                    ch.pipeline().addLast(new LineBasedFrameDecoder(maxPacketLong));
                                }
                                ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                                ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));


                                ch.pipeline().addLast(new NettyClientHandler(listener, mIndex, isSendheartBeat, heartBeatData, packetSeparator));
                            }
                        });
                try {
                    channelFuture = bootstrap.connect(host, tcp_port).addListener((ChannelFutureListener) channelFuture1 -> {
                        if (channelFuture1.isSuccess()) {
                            Log.e(TAG, "Connection succeeded");
                            reconnectNum = MAX_CONNECT_TIMES;
                            isConnect = true;
                            channel = channelFuture1.channel();
                        } else {
                            Log.e(TAG, "Connection failed");
                            isConnect = false;
                        }
                        isConnecting = false;
                    }).sync();

                    // Wait until the connection is closed.
                    channelFuture.channel().closeFuture().sync();
                    Log.e(TAG, "Disconnect");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isConnect = false;
                    listener.onClientStatusConnectChanged(ConnectState.STATUS_CONNECT_CLOSED, mIndex);
                    if (null != channelFuture) {
                        if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                            channelFuture.channel().close();
                        }
                    }
                    group.shutdownGracefully();
                    reconnect();
                }
            }
        }
    }


    public void disconnect() {
        Log.e(TAG, "Disconnect");
        isNeedReconnect = false;
        group.shutdownGracefully();
    }

    public void reconnect() {
        Log.e(TAG, "Reconnect");
        if (isNeedReconnect && reconnectNum > 0 && !isConnect) {
            reconnectNum--;
            SystemClock.sleep(reconnectIntervalTime);
            if (isNeedReconnect && reconnectNum > 0 && !isConnect) {
                Log.e(TAG, "Reconnect");
                connectServer();
            }
        }
    }

    /**
     * Send asynchronously
     *
     * @param data     the data to send
     * @param listener send result callback
     */
    public void sendMsgToServer(String data, final MessageStateListener listener) {
        boolean flag = channel != null && isConnect;
        if (flag) {
            String separator = TextUtils.isEmpty(packetSeparator) ? System.getProperty("line.separator") : packetSeparator;
            ChannelFuture channelFuture = channel.writeAndFlush(data + separator).addListener((ChannelFutureListener) channelFuture1 -> listener.isSendSuccss(channelFuture1.isSuccess()));
        }

//        ByteBuf buffer = Unpooled.copiedBuffer(data, Charset.forName("UTF-8"));
//        if (flag) {
//            channel.writeAndFlush(buffer).addListener(listener);
//        }
//        return flag;
//        byte[] bytes = strToByteArray(data);
//
//        return sendMsgToServer(bytes,listener);
    }

    /**
     * Send synchronously
     *
     * @param data the data to send
     * @return method execution result
     */
    public boolean sendMsgToServer(String data) {
        boolean flag = channel != null && isConnect;
        if (flag) {
            String separator = TextUtils.isEmpty(packetSeparator) ? System.getProperty("line.separator") : packetSeparator;
            ChannelFuture channelFuture = channel.writeAndFlush(data + separator).awaitUninterruptibly();
            return channelFuture.isSuccess();
        }
        return false;
    }


    public boolean sendMsgToServer(byte[] data, final MessageStateListener listener) {
        boolean flag = channel != null && isConnect;
        if (flag) {
            ByteBuf buf = Unpooled.copiedBuffer(data);
            channel.writeAndFlush(buf).addListener((ChannelFutureListener) channelFuture -> listener.isSendSuccss(channelFuture.isSuccess()));
        }
        return flag;
    }

    /**
     * Get TCP connection status
     *
     * @return get TCP connection status
     */
    public boolean getConnectStatus() {
        return isConnect;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public void setConnectStatus(boolean status) {
        this.isConnect = status;
    }

    public void setListener(NettyClientListener listener) {
        this.listener = listener;
    }

    public byte[] strToByteArray(String str) {
        if (str == null) {
            return null;
        }
        byte[] byteArray = str.getBytes();
        return byteArray;

    }

    /**
     * Builder, create NettyTcpClient
     */
    public static class Builder {

        /**
         * Maximum number of reconnections
         */
        private int MAX_CONNECT_TIMES = Integer.MAX_VALUE;

        /**
         * reconnection interval
         */
        private long reconnectIntervalTime = 5000;
        /**
         * server address
         */
        private String host;
        /**
         * server port
         */
        private int tcp_port;
        /**
         * Client ID, (since there may be multiple connections)
         */
        private int mIndex;

        /**
         * Whether to send heartbeat
         */
        private boolean isSendheartBeat;
        /**
         * heartbeat interval
         */
        private long heartBeatInterval = 5;

        /**
         * Heartbeat data, which can be String type or byte[].
         */
        private Object heartBeatData;

        private String packetSeparator;
        private int maxPacketLong = 1024;

        public Builder() {
            this.maxPacketLong = 1024;
        }


        public Builder setPacketSeparator(String packetSeparator) {
            this.packetSeparator = packetSeparator;
            return this;
        }

        public Builder setMaxPacketLong(int maxPacketLong) {
            this.maxPacketLong = maxPacketLong;
            return this;
        }

        public Builder setMaxReconnectTimes(int reConnectTimes) {
            this.MAX_CONNECT_TIMES = reConnectTimes;
            return this;
        }


        public Builder setReconnectIntervalTime(long reconnectIntervalTime) {
            this.reconnectIntervalTime = reconnectIntervalTime;
            return this;
        }


        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setTcpPort(int tcp_port) {
            this.tcp_port = tcp_port;
            return this;
        }

        public Builder setIndex(int mIndex) {
            this.mIndex = mIndex;
            return this;
        }

        public Builder setHeartBeatInterval(long intervalTime) {
            this.heartBeatInterval = intervalTime;
            return this;
        }

        public Builder setSendheartBeat(boolean isSendheartBeat) {
            this.isSendheartBeat = isSendheartBeat;
            return this;
        }

        public Builder setHeartBeatData(Object heartBeatData) {
            this.heartBeatData = heartBeatData;
            return this;
        }

        public NettyTcpClient build() {
            NettyTcpClient nettyTcpClient = new NettyTcpClient(host, tcp_port, mIndex);
            nettyTcpClient.MAX_CONNECT_TIMES = this.MAX_CONNECT_TIMES;
            nettyTcpClient.reconnectIntervalTime = this.reconnectIntervalTime;
            nettyTcpClient.heartBeatInterval = this.heartBeatInterval;
            nettyTcpClient.isSendheartBeat = this.isSendheartBeat;
            nettyTcpClient.heartBeatData = this.heartBeatData;
            nettyTcpClient.packetSeparator = this.packetSeparator;
            nettyTcpClient.maxPacketLong = this.maxPacketLong;
            return nettyTcpClient;
        }
    }
}
