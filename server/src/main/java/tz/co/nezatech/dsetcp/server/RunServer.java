package tz.co.nezatech.dsetcp.server;


import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.RSAUtil;
import tz.co.nezatech.dsetp.util.RandomUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.TestConsts;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.handler.MyHttpHandler;
import tz.co.nezatech.dsetp.util.handler.RequestHandler;
import tz.co.nezatech.dsetp.util.handler.http.MsgReadHandler;
import tz.co.nezatech.dsetp.util.handler.http.MsgTypeHandler;
import tz.co.nezatech.dsetp.util.handler.http.StreamSectionsHandler;
import tz.co.nezatech.dsetp.util.handler.http.SwitchEndianHandler;
import tz.co.nezatech.dsetp.util.message.MessageReader;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
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
        RunServer server = new RunServer(args);
        server.runHttp();
        server.init();
    }

    private void runHttp() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(Integer.parseInt(conCfg.getProperty("http.server.port"))), 0);
            server.createContext("/", new MyHttpHandler(new RequestHandler[]{
                    new MsgTypeHandler(),
                    new MsgReadHandler(),
                    new SwitchEndianHandler(),
                    new StreamSectionsHandler()
            }));
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    logger.debug("Send Daily Key: " + ack);
                    output.write(ack.getMessage());
                    output.flush();

                    while (con.isConnected() && !isStopped()) {
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
        boolean stopped = false;

        boolean isStopped() {
            return stopped;
        }

        @Override
        public void process(byte[] fullMsg, String id, byte[] mh, byte[] msg, OutputStream output) {
            int seqNo = TCPUtil.seqNo(mh);
            String username = TCPUtil.getUsername(mh).trim();
            String userNo = TCPUtil.getUserNo(mh).trim();
            String time = TCPUtil.getTime(mh);
            logger.debug(String.format("<< Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
            logger.debug("<< Message: " + TCPUtil.text(fullMsg));
            MessageType type = MessageType.byType(mh[28]);
            logger.debug("<< MessageType: " + type);
            if (type == MessageType.LOGIN) {
                sendMessage(TCPUtil.hexToBytes(TestConsts.successLoginReponse), MessageType.ACK_SUCCESS, output);
                //enable heartbeat
                enableHeartbeat(id, output);
            } else if (type == MessageType.LOG_OUT) {
                stopped = true;
            }
        }

        public void enableHeartbeat(String id, OutputStream output) {
            try {
                String file = "D:\\TempFiles\\randommsges.txt";
                StringWriter writer = new StringWriter();
                IOUtils.copy(new FileInputStream(new File(file)), writer, "utf-8");
                writer.flush();

                String s = writer.toString().replaceAll("\n\r", "");
                String dummy[] = s.split(",");

                final Runnable hb = () -> {
                    logger.info(">> Sending heartbeat for " + id);
                    try {

                        if (stopped) {
                            scheduler.shutdownNow();
                            return;
                        }

                        TCPMessage ack = frmConfig("".getBytes(), ++sequence, MessageType.HEART_BEAT_SVR);
                        sendMessage(ack, MessageType.HEART_BEAT_SVR, output);

                        //send random msg
                        pause(2);
                        int i = RandomUtil.get(0, dummy.length - 1);
                        String msgHex = dummy[i];
                        byte[] bytes = TCPUtil.hexToBytes(msgHex);
                        sendMessage(bytes, TCPUtil.msgType(bytes), output);
                    } catch (Exception ex) {
                        logger.debug("Exception: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                };

                scheduler.scheduleAtFixedRate(hb, 30, 30, SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        private void sendMessage(byte[] msg, MessageType type, OutputStream output) {
            TCPMessage outMsg = frmConfig(msg, 2, type);
            sendMessage(outMsg, type, output);
        }

        private void sendMessage(TCPMessage outMsg, MessageType type, OutputStream output) {
            String text = TCPUtil.text(outMsg.getMessage());
            logger.debug(">> Sending message(HEX): " + type + " = " + text);

            try {
                output.write(outMsg.getMessage());
                output.flush();
            } catch (IOException e) {
                stopped = true;
                scheduler.shutdownNow();
                logger.error(">> Failed to send msg to client: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private TCPMessage frmConfig(byte[] msg, int seq, MessageType type) {
            String username = conCfg.getProperty("sender.username");
            int userNo = Integer.parseInt(conCfg.getProperty("sender.user.number"));
            //logger.debug(String.format("Username: %s, UserNo: %d", username, userNo));
            return new TCPMessage(msg, seq, username, userNo, TCPUtil.timeNow(), type.getType());
        }
    }
}
