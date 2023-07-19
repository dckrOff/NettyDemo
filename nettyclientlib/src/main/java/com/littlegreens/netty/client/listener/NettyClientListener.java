package com.littlegreens.netty.client.listener;


/**
 * Created by littleGreens on 2018-11-10.
 * TCP state change monitoring
 */
public interface NettyClientListener<T> {

    /**
     * When a system message is received
     * @param msg the message
     * @param index tcp client identification, because an application may have many long links
     */
    void onMessageResponseClient(T msg, int index);

    /**
     * Triggered when the service state changes
     * @param statusCode status change
     * @param index tcp client identification, because an application may have many long links
     */
    public void onClientStatusConnectChanged(int statusCode, int index);
}
