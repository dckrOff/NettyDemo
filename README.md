# NettyDemo
* This is a clone of the repository https://github.com/aLittleGreens/NettyDemo

 > Netty is a network application framework based on Java NIO client-server, using Netty can quickly develop network applications
 
    > For more usage, please jump to https://github.com/netty/netty
 
  **This project is based on the project built by Netty on the Android platform. It only provides the usage method of Netty. You can customize it according to your own needs. **
 
  **During the demonstration, the client can modify the IP address of the TCP server in Const.java. The server can communicate with multiple clients by switching channels. **
 
  Finally, if there are any deficiencies, please ask Haihan to raise more issues, and we will solve them together.
  
  ## How to import
 
```
 dependencies {
  implementation 'com.littlegreens.netty.client:nettyclientlib:1.0.5'
 } 
```
## 1. First look at the demonstration effect, followed by a detailed usage tutorial
  <img src="https://github.com/cai784921129/NettyDemo/blob/master/screenshot/clent.gif" width="280px"/> <img src="https://github.com/cai784921129/NettyDemo /blob/master/screenshot/server.gif" height="280px"/>

If it is used as a TCP client, you can directly rely on

## 2, HOW TO USE?

1. **Create TCP client**
```Java
     NettyTcpClient mNettyTcpClient = new NettyTcpClient.Builder()
                 .setHost(Const.HOST)                       // Set server address
                 .setTcpPort(Const.TCP_PORT)                // Set the server port number
                 .setMaxReconnectTimes(5)                   // Set the maximum number of reconnections
                 .setReconnectIntervalTime(5)               // Set the reconnection interval. Unit: second
                 .setSendheartBeat(true)                    // Set whether to send heartbeat
                 .setHeartBeatInterval(5)                   // Set the heartbeat interval. Unit: second
                 .setHeartBeatData("I'm is HeartBeatData")  // Set the heartbeat data, which can be String type or byte[], whichever is set later
                 .setIndex(0)                               // Set the client ID. (Because there may be multiple tcp connections)
//               .setPacketSeparator("#")                   // Use special characters as separators to solve the problem of sticky packets. The default is to use newline characters as separators
//               .setMaxPacketLong(1024)                    // Set the maximum length of data sent once, the default is 1024
                 .build();
```

2. **Set up monitoring**
```Java
        mNettyTcpClient.setListener(new NettyClientListener<String>() {
            @Override
            public void onMessageResponseClient(String msg, int index) {
                // Message callback from the server
            }

            @Override
            public void onClientStatusConnectChanged(int statusCode, int index) {
               // connection status callback
            }
        });
```
3. **connect, disconnect**
- Determine whether it is connected
```Java
mNettyTcpClient.getConnectStatus()
```
- Сonnect
```Java
mNettyTcpClient.connect();
```
- Disconnect
```Java
mNettyTcpClient.disconnect();
```
4. ** Send information to the server **
```Java
                    mNettyTcpClient.sendMsgToServer(msg, new MessageStateListener() {
                        @Override
                        public void isSendSuccss(boolean isSuccess) {
                            if (isSuccess) {
                                Log.d(TAG, "send successful");
                            } else {
                                Log.d(TAG, "send error");
                            }
                        }
                    });
```

## Second, the problems encountered by the small partners
1. The message fed back by the server is truncated
Answer: Since the socket will stick packets, the sdk uses a special symbol as the separator by default to solve the problem of sticking packets. When the client sends information, the sdk will add a delimiter at the end. Here you need to pay attention to the server. When returning information, the corresponding delimiter should also be added.

2. The master branch code does not support sending byte[] format, please refer to the develop_2.0 branch for the requirement of byte[] format


