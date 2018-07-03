package tz.co.nezatech.dsetcp.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.message.MessageReader;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class RunServer {
    Logger logger = LoggerFactory.getLogger(RunServer.class.getName());
    Config conCfg = ConnectionConfig.get("server.cfg");
    int clientId = 0;


    public static void main(String[] args) {
        RunServer client = new RunServer();
        client.init();
    }

    void init() {
        logger.debug("Starting TCP/IP Server");
        String ip = conCfg.getProperty("tcp.server.host");
        String port = conCfg.getProperty("tcp.server.port");
        String compId = conCfg.getProperty("sender.comp.id");
        String pwd = conCfg.getProperty("sender.password");
        logger.debug(String.format("Server: %s:%s ", ip, port));
        logger.debug(String.format("Client: %s ", compId));

        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
            logger.error(String.format("Server started successfully at %s port", port));
            logger.info(String.format("Waiting for connections at %s port", port));
            while (true) {
                Socket con = serverSocket.accept();
                String host = con.getInetAddress().getHostAddress();
                logger.debug(String.format("Host %s is connected successfully", host));
                handleNewClient(con);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
        }
    }

    private void handleNewClient(final Socket con) {
        clientId++;
        Thread handler = new Thread(new Handler() {
            @Override
            public void run() {
                logger.debug("Handling new client");
                int sequence = 0;
                try {
                    OutputStream output = con.getOutputStream();
                    PrintWriter writer = new PrintWriter(output, true);

                    String text="Welcome, You can proceed with Login";
                    TCPMessage ack = frmConfig(text, ++sequence, MessageType.ACK_SUCCESS);
                    logger.debug("Send ACK SUCCESS Message: " + ack);
                    output.write(ack.getMessage());
                    output.flush();

                    while (con.isConnected()) {
                        InputStream input = con.getInputStream();
                        int avail = input.available();
                        if (avail != 0) {
                            logger.debug("Some data to read: " + avail);
                            readMessage("CLIENT => " + clientId, input, output);
                        }

                        pause(2);
                    }
                    logger.debug("Client disconnected");
                    con.close();
                } catch (IOException ex) {
                    logger.error(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        handler.start();
    }

    private void pause(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TCPMessage frmConfig(String msg, int seq, MessageType type) {
        return new TCPMessage(msg.getBytes(), seq, conCfg.getProperty("sender.username"), conCfg.getProperty("sender.user.id"), TCPUtil.timeNow(), type.getType());
    }

    abstract class Handler extends MessageReader implements Runnable {
        @Override
        public void process(String id, byte[] mh, byte[] msg, OutputStream output) {
            logger.debug("Server => MH Size: " + mh.length);
            TCPUtil.print(mh);
            TCPUtil.print(msg);
            int seqNo = TCPUtil.seqNo(mh);
            String username = TCPUtil.getUsername(mh);
            String userNo = TCPUtil.getUserNo(mh);
            String time = TCPUtil.getTime(mh);
            logger.info(String.format("Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
            logger.info("Message: " + new String(msg));
        }
    }
}
