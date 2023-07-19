package ifreecomm.nettyserver.netty;


import io.netty.channel.Channel;


public interface NettyServerListener<T> {

    public final static byte STATUS_CONNECT_SUCCESS = 1;

    public final static byte STATUS_CONNECT_CLOSED = 0;

    public final static byte STATUS_CONNECT_ERROR = 0;

    /**
     *
     * @param msg
     * @param ChannelId unique id
     */
    void onMessageResponseServer(T msg,String ChannelId);

    /**
     * server started successfully
     */
    void onStartServer();

    /**
     * server shut down
     */
    void onStopServer();

    /**
     * Establish a connection with the client
     *
     * @param channel
     */
    void onChannelConnect(Channel channel);

    /**
     * Disconnect from client
     * @param
     */
    void onChannelDisConnect(Channel channel);

}
