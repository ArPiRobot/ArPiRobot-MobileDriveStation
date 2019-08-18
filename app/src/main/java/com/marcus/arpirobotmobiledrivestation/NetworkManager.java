package com.marcus.arpirobotmobiledrivestation;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NetworkManager {

    // Networking settings
    public final static int CONTROLLER_PORT = 8090;
    public final static int COMMAND_PORT = 8091;
    public final static int NET_TABLE_PORT = 8092;
    public final static int LOG_PORT = 8093;
    public final static int CONNECT_TIMEOUT = 5000; // ms

    // Commands that can be sent to the robot
    public final static String COMMAND_ENABLE = "ENABLE";
    public final static String COMMAND_DISABLE = "DISABLE";
    public final static String COMMAND_NET_TABLE_SYNC = "NT_SYNC";

    public final static byte[] NT_SYNC_START_DATA = new byte[] {(byte)255, (byte)255, '\n'};
    public final static byte[] NT_SYNC_STOP_DATA = new byte[] {(byte)255, (byte)255, (byte)255, '\n'};

    private boolean hasRespondedToDisconnect = false;
    private boolean isConnected = false;

    // Net table data
    private List<Byte> netTableReadBuffer = new ArrayList<>();
    private ArrayList<String> syncKeys = new ArrayList<>();
    private ArrayList<String> syncValues = new ArrayList<>();
    private boolean netTableInSync = false;

    private String logBuffer = "";

    private EventLoopGroup workerGroup;
    private Channel commandChannel, netTableChannel, logChannel;
    private DatagramSocket controllerSocket;

    private ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);

    public void connect(String address) {

        // Reset state
        if(isConnected) return;
        hasRespondedToDisconnect = false;

        // Reset net table data state
        netTableReadBuffer.clear();
        netTableInSync = false;
        syncKeys.clear();
        syncValues.clear();

        logBuffer = "";

        // Do not block while bootstrapping
        asyncExecutor.execute(() -> {
            workerGroup = new NioEventLoopGroup();

            Bootstrap cmdBootstrap = new Bootstrap();
            cmdBootstrap.group(workerGroup);
            cmdBootstrap.channel(NioSocketChannel.class);
            cmdBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
            cmdBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addLast(new StringEncoder(StandardCharsets.UTF_8))
                            .addLast(new StringDecoder(StandardCharsets.UTF_8))
                            .addLast(new CommandHandler());
                }
            });

            Bootstrap netTableBootstrap = new Bootstrap();
            netTableBootstrap.group(workerGroup);
            netTableBootstrap.channel(NioSocketChannel.class);
            netTableBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
            netTableBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addLast(new ByteArrayEncoder())
                            .addLast(new ByteArrayDecoder())
                            .addLast(new NetTableHandler());
                }
            });

            Bootstrap logBootstrap = new Bootstrap();
            logBootstrap.group(workerGroup);
            logBootstrap.channel(NioSocketChannel.class);
            logBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
            logBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addLast(new StringEncoder(StandardCharsets.UTF_8))
                            .addLast(new StringDecoder(StandardCharsets.UTF_8))
                            .addLast(new LogHandler());
                }
            });

            String host = MainActivity.instance.getRobotAddress();
            ChannelFuture cmdChannelFuture = cmdBootstrap.connect(host, COMMAND_PORT);
            ChannelFuture netTableChannelFuture = netTableBootstrap.connect(host, NET_TABLE_PORT);
            ChannelFuture logChannelFuture = logBootstrap.connect(host, LOG_PORT);

            // Wait for all to connect (don't use sync b/c it won't allow connect timeout to work)
            try {
                cmdChannelFuture.await();
                netTableChannelFuture.await();
                logChannelFuture.await();
            }catch(InterruptedException e) {

            }

            if(cmdChannelFuture.isCancelled() ||
                    netTableChannelFuture.isCancelled() ||
                    logChannelFuture.isCancelled()) {
                return; // Connect canceled
            }

            if(cmdChannelFuture.isDone() && cmdChannelFuture.isSuccess()) {
                commandChannel = cmdChannelFuture.channel();
            }

            if(netTableChannelFuture.isDone() && netTableChannelFuture.isSuccess()) {
                netTableChannel = netTableChannelFuture.channel();
            }

            if(logChannelFuture.isDone() && logChannelFuture.isSuccess()) {
                logChannel = logChannelFuture.channel();
            }

            // Make sure both connected. If not disconnect.
            if(commandChannel == null || netTableChannel == null || logChannel == null) {
                MainActivity.instance.logError("Failed to connect to one or more ports.");
                doDisconnect();
                MainActivity.instance.runOnUiThread(() -> {
                    MainActivity.instance.failedToConnectToRobot();
                });
                return;
            }

            // Setup UDP for controller port
            try {
                controllerSocket = new DatagramSocket();
                controllerSocket.connect(new InetSocketAddress(
                        MainActivity.instance.getRobotAddress(),
                        CONTROLLER_PORT
                ));
            } catch (SocketException e) {
                // Failed to setup UDP (for some unknown reason)
                doDisconnect();
                MainActivity.instance.runOnUiThread(() -> {
                    MainActivity.instance.failedToConnectToRobot();
                });
                return;
            }

            // Connected successfully
            isConnected = true;
            MainActivity.instance.runOnUiThread(() -> {
                MainActivity.instance.connectedToRobot();
            });

            // Already running async
            periodicConnectivityCheck();

        });
    }

    public void disconnect(boolean userInitiated) {
        if(!isConnected || hasRespondedToDisconnect) return;
        hasRespondedToDisconnect = true;

        // Don't block while closing
        asyncExecutor.execute(() -> {
            doDisconnect();
            MainActivity.instance.runOnUiThread(() -> {
                MainActivity.instance.disconnectedFromRobot(userInitiated);
            });
        });
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendControllerData(byte[] data) {
        if(isConnected) {
            try {
                controllerSocket.send(new DatagramPacket(data, data.length));
            } catch (IOException e) {
                // UDP channel is for some reason closed (this should never happen)
                if(!hasRespondedToDisconnect) {
                    MainActivity.instance.logError("Failed to write to controller port.");
                }
                disconnect(false);
            }
        }
    }

    public void sendCommand(String command) {
        if(isConnected) {
            ChannelFuture f = commandChannel.writeAndFlush(command);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()) {
                        // Write failed. Disconnect.
                        if(!hasRespondedToDisconnect)
                            MainActivity.instance.logError("Failed to write to command port.");
                        disconnect(false);
                    }
                }
            });
        }
    }

    public void sendNTKey(String key, String value) {
        byte[] data = new byte[key.length() + value.length() + 2];
        System.arraycopy(key.getBytes(StandardCharsets.UTF_8), 0, data, 0, key.length());
        data[key.length()] = (byte)255;
        System.arraycopy(value.getBytes(StandardCharsets.UTF_8), 0, data, key.length() + 1, value.length());
        data[data.length - 1] = '\n';
        sendNTRaw(data);
    }

    public void sendNTRaw(byte[] data) {
        if(isConnected) {
            ChannelFuture f = netTableChannel.writeAndFlush(data);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()) {
                        // Write failed. Disconnect.
                        if(!hasRespondedToDisconnect)
                            MainActivity.instance.logError("Failed to write to command port.");
                        disconnect(false);
                    }
                }
            });
        }
    }


    private void doDisconnect() {
        if(!isConnected) return;

        isConnected = false;

        //DriveStation.netTable.abortSync(false);

        if(commandChannel != null)
            commandChannel.close();

        if(netTableChannel != null)
            netTableChannel.close();

        if(controllerSocket != null)
            controllerSocket.close();

        if(logChannel != null)
            logChannel.close();

        try {
            if(commandChannel != null)
                commandChannel.closeFuture().sync();
            if(netTableChannel != null)
                netTableChannel.closeFuture().sync();
            if(logChannel != null)
                logChannel.closeFuture().sync();
        } catch (InterruptedException e) {

        }

        commandChannel = null;
        netTableChannel = null;
        controllerSocket = null;
        logChannel = null;

        workerGroup.shutdownGracefully();
        workerGroup = null;

    }

    private void handleNetTableData(byte[] newData) {
        List<Byte> newDataList = Bytes.asList(newData);
        netTableReadBuffer.addAll(newDataList);

        int endPos = -1;

        while((endPos = netTableReadBuffer.indexOf((byte)'\n')) != -1) {
            List<Byte> subset = netTableReadBuffer.subList(0, endPos + 1);
            if(matchData(subset, NT_SYNC_START_DATA)) {
                //netTableInSync = true;
                //DriveStation.netTable.startSync();
            }else if(matchData(subset, NT_SYNC_STOP_DATA)) {
                //DriveStation.netTable.finishSync(syncKeys, syncValues);
                //netTableInSync = false;
            }else {
                int delimPos = netTableReadBuffer.indexOf((byte)255);

                if(delimPos >= 0 && delimPos < endPos) {
                    // Valid data
                    List<Byte> keySubset = netTableReadBuffer.subList(0, delimPos);
                    List<Byte> valueSubset = netTableReadBuffer.subList(delimPos + 1, endPos);
                    String key = new String(Bytes.toArray(keySubset), StandardCharsets.UTF_8);
                    String value = new String(Bytes.toArray(valueSubset), StandardCharsets.UTF_8);

                    if(netTableInSync) {
                        syncKeys.add(key);
                        syncValues.add(value);
                    }else {
                        //DriveStation.netTable.setFromRobot(key, value);
                    }
                }
            }

            netTableReadBuffer = netTableReadBuffer.subList(endPos + 1, netTableReadBuffer.size());

        }
    }

    private boolean matchData(List<Byte> dataSubset, byte[] dataToMatch) {
        if(dataSubset.size() != dataToMatch.length)
            return false;

        for(int i = 0; i < dataToMatch.length; ++i) {
            if(dataSubset.get(i) != dataToMatch[i])
                return false;
        }
        return true;
    }


    @SuppressWarnings("unused")
    private void printBytes(List<Byte> subset) {
        String str = "";
        for(Byte b : subset) {
            str += (byte)b;
            str += ",";
        }
        System.out.println("[PRINT_DATA]: " + str);
    }

    @SuppressWarnings("unused")
    private void printBytes(byte[] subset) {
        String str = "";
        for(byte b : subset) {
            str += b;
            str += ",";
        }
        System.out.println("[PRINT_DATA]: " + str);
    }

    private void periodicConnectivityCheck() {
        while(isConnected) {
            // If the remote host closes the connection an exception is not always fired, but
            //   isOpen will return false. Trigger a disconnect event if this occurs.
            if(!commandChannel.isOpen()) {
                // Log the first failure
                if(!hasRespondedToDisconnect) {
                    MainActivity.instance.logError("Command client - Connection closed by remote host.");
                }

                // Closed by remote endpoint
                disconnect(false);
                break;
            }else if(!netTableChannel.isOpen()) {
                // Log the first failure
                if(!hasRespondedToDisconnect) {
                    MainActivity.instance.logError("Net table client - Connection closed by remote host.");
                }

                // Closed by remote endpoint
                disconnect(false);
                break;
            }else if(!logChannel.isOpen()) {
                // Log the first failure
                if(!hasRespondedToDisconnect) {
                    MainActivity.instance.logError("Log client - Connection closed by remote host.");
                }

                // Closed by remote endpoint
                disconnect(false);
                break;
            }

            try {
                Thread.sleep(250);
            }catch(InterruptedException e) {

            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// TCP Client Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////

    class CommandHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // There should not be any data sent to the DS on the command port
            MainActivity.instance.logWarning("Robot sent data over the command port. It will be ignored.");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // Log the first failure
            if(!hasRespondedToDisconnect) {
                MainActivity.instance.logError("Failed to write to command port.");
                MainActivity.instance.logDebug("Exception: " + cause.getMessage());
            }
            // Closed by remote endpoint
            disconnect(false);
        }
    }

    class NetTableHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            handleNetTableData((byte[])msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // Log the first failure
            if(!hasRespondedToDisconnect) {
                MainActivity.instance.logError("Failed to write to net table port.");
                MainActivity.instance.logDebug("Exception: " + cause.getMessage());
            }

            // Closed by remote endpoint
            disconnect(false);
        }
    }

    class LogHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            logBuffer += msg.toString();
            if(logBuffer.indexOf('\n') != -1) {
                String[] messages = logBuffer.split("\n");
                int end = messages.length;
                if(!logBuffer.endsWith("\n")) {
                    end -= 1;
                    logBuffer = messages[messages.length - 1];
                }else {
                    logBuffer = "";
                }
                for(int i = 0; i < end; ++i) {
                    MainActivity.instance.handleRobotLog(messages[i]);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // Log the first failure
            if(!hasRespondedToDisconnect) {
                MainActivity.instance.logError("Log client error.");
                MainActivity.instance.logDebug("Exception: " + cause.getMessage());
            }

            // Closed by remote endpoint
            disconnect(false);
        }
    }
}
