package com.littlegreens.netty.client.handler;

import android.text.TextUtils;
import android.util.Log;

import com.littlegreens.netty.client.listener.NettyClientListener;
import com.littlegreens.netty.client.status.ConnectState;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

    private static final String TAG = "NettyClientHandler";
    private final boolean isSendheartBeat;
    private NettyClientListener listener;
    private int index;
    private Object heartBeatData;
    private String packetSeparator;

    //    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat"+System.getProperty("line.separator"),
//            CharsetUtil.UTF_8));
    byte[] requestBody = {(byte) 0xFE, (byte) 0xED, (byte) 0xFE, 5, 4, (byte) 0xFF, 0x0a};


    public NettyClientHandler(NettyClientListener listener, int index, boolean isSendheartBeat, Object heartBeatData) {
        this(listener,index,isSendheartBeat,heartBeatData,null);
    }

    public NettyClientHandler(NettyClientListener listener, int index, boolean isSendheartBeat, Object heartBeatData,String separator) {
        this.listener = listener;
        this.index = index;
        this.isSendheartBeat = isSendheartBeat;
        this.heartBeatData = heartBeatData;
        this.packetSeparator = TextUtils.isEmpty(separator) ? System.getProperty("line.separator") : separator;
    }

    /**
     * <p>Set the IdleStateHandler heartbeat detection to perform a read detection every x seconds,
     * If the ChannelRead() method is not called within x seconds, the userEventTrigger() method will be triggered once </p>
     *
     * @param ctx ChannelHandlerContext
     * @param evt IdleStateEvent
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {   // send heartbeat
//                ctx.channel().writeAndFlush("Heartbeat" + System.getProperty("line.separator"));
                if (isSendheartBeat) {
                    if (heartBeatData == null) {
                        ctx.channel().writeAndFlush("Heartbeat" + packetSeparator);
                    } else {
                        if (heartBeatData instanceof String) {
//                            Log.d(TAG, "userEventTriggered: String");
                            ctx.channel().writeAndFlush(heartBeatData + packetSeparator);
                        } else if (heartBeatData instanceof byte[]) {
//                            Log.d(TAG, "userEventTriggered: byte");
                            ByteBuf buf = Unpooled.copiedBuffer((byte[]) heartBeatData);
                            ctx.channel().writeAndFlush(buf);
                        } else {
                            Log.e(TAG, "userEventTriggered: heartBeatData type error");
                        }
                    }
                } else {
                    Log.e(TAG, "No heartbeat");
                }
            }
        }
    }

    /**
     * <p>Client online</p>
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Log.e(TAG, "channelActive");
//        NettyTcpClient.getInstance().setConnectStatus(true);
        listener.onClientStatusConnectChanged(ConnectState.STATUS_CONNECT_SUCCESS, index);
    }

    /**
     * <p>Client offline</p>
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Log.e(TAG, "channelInactive");
//        NettyTcpClient.getInstance().setConnectStatus(false);
//        listener.onServiceStatusConnectChanged(NettyClientListener.STATUS_CONNECT_CLOSED);
        // NettyTcpClient.getInstance().reconnect();
    }

    /**
     * The client receives the message
     *
     * @param channelHandlerContext ChannelHandlerContext
     * @param msg the message
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) {
        Log.e(TAG, "channelRead0:"+msg);
        listener.onMessageResponseClient(msg, index);
    }

    /**
     * @param ctx ChannelHandlerContext
     * @param cause exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
//        NettyTcpClient.getInstance().setConnectStatus(false);
        Log.e(TAG, "exceptionCaught");
        listener.onClientStatusConnectChanged(ConnectState.STATUS_CONNECT_ERROR, index);
        cause.printStackTrace();
        ctx.close();
    }
}
