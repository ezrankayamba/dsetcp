package tz.co.nezatech.dsetcp.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.RSAUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.message.MessageReader;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RunServer {
    Logger logger = LoggerFactory.getLogger(RunServer.class.getName());
    Config conCfg = ConnectionConfig.get("server.cfg");
    int clientId = 0;
    KeyPair keyPair;
    private String[] args;

    public RunServer(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        RunServer client = new RunServer(args);
        client.init();
    }

    void init() {
        logger.debug("Starting TCP/IP Server");
        String port = conCfg.getProperty("tcp.server.port");
        if (args.length > 0) {
            port = args[0];
        }
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);

            keyPair = kpg.generateKeyPair();
        } catch (Exception ex) {

        }

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

    private void handleNewClient(final Socket con) throws IOException {
        clientId++;
        String id = "CLIENT: " + clientId;
        Thread handler = new Thread(new Handler() {
            @Override
            public void run() {
                logger.debug("Handling new client");
                int sequence = 0;
                try {
                    OutputStream output = con.getOutputStream();

                    RSAPublicKey pk = (RSAPublicKey) keyPair.getPublic();
                    String xml = RSAUtil.getPublicKeyAsXml(pk);
                    TCPMessage ack = frmConfig(xml, ++sequence, MessageType.DAILY_KEY);
                    logger.debug("Send ACK SUCCESS Message: " + ack);
                    output.write(ack.getMessage());
                    output.flush();

                    //enable heartbeat
                    enableHeartbeat(id, output);

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
        return new TCPMessage(msg.getBytes(), seq, conCfg.getProperty("sender.username"), Integer.parseInt(conCfg.getProperty("sender.user.number")), TCPUtil.timeNow(), type.getType());
    }

    abstract class Handler extends MessageReader implements Runnable {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int sequence = 0;

        @Override
        public void process(String id, byte[] mh, byte[] msg, OutputStream output) {
            logger.debug("ID: " + id);
            int seqNo = TCPUtil.seqNo(mh);
            String username = TCPUtil.getUsername(mh).trim();
            String userNo = TCPUtil.getUserNo(mh).trim();
            String time = TCPUtil.getTime(mh);
            logger.debug(String.format("Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
            logger.debug("Message: " + new String(msg));
            MessageType type = MessageType.byType(mh[28]);
            logger.debug("MessageType: " + type);
        }

        public void enableHeartbeat(String id, OutputStream output) {
            final Runnable hb = () -> {
                logger.info("Sending heartbeat for " + id);
                try {
                    TCPMessage ack = frmConfig("", ++sequence, MessageType.HEART_BEAT_SVR);
                    logger.debug("Send ACK SUCCESS Message: " + ack);
                    output.write(ack.getMessage());
                    output.flush();
                } catch (Exception ex) {
                    logger.debug("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };
            final ScheduledFuture<?> schedule = scheduler.scheduleAtFixedRate(hb, 30, 30, SECONDS);
        }
    }
}
