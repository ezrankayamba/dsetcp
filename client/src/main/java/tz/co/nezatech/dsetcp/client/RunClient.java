package tz.co.nezatech.dsetcp.client;

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
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;


public class RunClient extends MessageReader {
    Logger logger = LoggerFactory.getLogger(RunClient.class.getName());
    Config conCfg;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    short appState = 0;
    int msgSequence = 0;
    private String[] args;

    public RunClient(String[] args) {
        super();
        this.args = args;
        String profile = System.getenv("profile");
        logger.info("Profile: " + profile);
        if (profile != null && !profile.trim().isEmpty() && profile.trim().equalsIgnoreCase("test")) {
            conCfg = ConnectionConfig.get("client-test.cfg");
        } else {
            conCfg = ConnectionConfig.get("client.cfg");
        }
    }

    public static void main(String[] args) {
        RunClient client = new RunClient(args);
        client.init();
    }

    void init() {
        logger.debug("Starting TCP/IP connection");
        String ip = conCfg.getProperty("tcp.server.host");
        String port = conCfg.getProperty("tcp.server.port");
        try {
            try (Socket con = new Socket(ip, Integer.parseInt(port))) {
                logger.debug(String.format("Successfully connected to server. IP: %s, Port: %s. ", ip, port));
                enableHeartbeat(String.format("%s:%s", ip, port), con.getOutputStream());
                while (true) {
                    if (!con.isConnected()) {
                        logger.error("Socket closed, contact support");
                        System.exit(-1);
                    }

                    InputStream input = con.getInputStream();
                    int avail = input.available();

                    if (avail != 0) {
                        logger.debug("Some data to read: " + avail);
                        readMessage("0", input, con.getOutputStream());
                    } else {
                        logger.debug("Waiting for data...");
                    }
                    pause(2);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

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
        logger.debug("App State: " + appState);
        logger.debug("MessageType: " + type);
        if (appState == 0) {//Just connected
            if (type == MessageType.DAILY_KEY) {
                appState = 1;

                //send login message
                this.msgSequence++;
                try {
                    sendLogin(msg, output);
                    logger.debug("Login message sent");
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                    logger.error("Failed to login to DSE. Check logs or contact support");
                    e.printStackTrace();
                    System.exit(-1);
                }
            } else {
                logger.error("Failed to connect to DSE. Check logs or contact support");
                System.exit(-1);
            }
        } else if (appState == 1) {//Login response
            logger.debug("Login response: " + new String(msg));
            if (type == MessageType.ACK_SUCCESS) {
                logger.debug("Login success");

                //subscribe - Request Daily Trend Msg Type 61
                //sendMessage(conCfg.getProperty("sender.contract.name"), MessageType.START_OF_DAY_DOWNLOAD, output);

                LocalDateTime now = LocalDateTime.now();
                int day = now.getDayOfMonth();
                int month = now.getMonthValue();
                int year = now.getYear();

                byte[] sodd = new byte[24];
                sodd[0] = (byte) 1; // data type
                sodd[1] = (byte) 0; // unused
                sodd[2] = (byte) 0; // last piece of chunk
                sodd[3] = (byte) 0; //re-request
                System.arraycopy(TCPUtil.intToBytes(0), 0, sodd, 4, 4); // action
                System.arraycopy(TCPUtil.intToBytes(0), 0, sodd, 8, 4); // specific record
                System.arraycopy(TCPUtil.intToBytes(year), 0, sodd, 12, 4); // year
                System.arraycopy(TCPUtil.intToBytes(month), 0, sodd, 16, 4); // month
                System.arraycopy(TCPUtil.intToBytes(day), 0, sodd, 20, 4); // day
                sendMessage(sodd, MessageType.START_OF_DAY_DOWNLOAD, output);
                sendMessage(new byte[]{(byte)0}, MessageType.REQUEST_SCREEN_OPEN, output);
            } else if (type == MessageType.ACK_ERROR) {
                logger.error("Login failed");
            } else {
                logger.debug("Login status unknown: " + type);
            }
        } else {
            logger.debug("Application message: " + new String(msg));
        }
    }

    private void sendLogin(byte[] msg, OutputStream output) throws IOException {
        TCPMessage login = login(msgSequence, msg);
        byte[] msgToSend = login.getMessage();
        logger.debug(String.format("Login Message: %s ", TCPUtil.text(msgToSend)));
        output.write(msgToSend);
        output.flush();
    }

    public void enableHeartbeat(String id, OutputStream output) {
        final Runnable hb = () -> {
            logger.info("Sending heartbeat for " + id);
            sendMessage("", MessageType.HEART_BEAT_CLT, output);
        };
        final ScheduledFuture<?> schedule = scheduler.scheduleAtFixedRate(hb, 30, 30, SECONDS);
    }

    private void sendMessage(String msg, MessageType type, OutputStream output) {
        sendMessage(msg.getBytes(),type, output);
    }
    private void sendMessage(byte[] msg, MessageType type, OutputStream output) {
        TCPMessage ack = frmConfig(msg, ++msgSequence, type);
        logger.debug("Send Heartbeat Message: " + ack);
        try {
            output.write(ack.getMessage());
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TCPMessage login(int seq, byte[] key) {
        try {
            String keyStr = new String(key);
            logger.debug("Key: " + keyStr);
            String pwd = conCfg.getProperty("sender.password");
            logger.debug("Plain pwd: " + pwd);
            byte[] encrypt = RSAUtil.encryptFromXmlKey(pwd, new String(key));
            logger.debug("Encrypted pwd: " + new String(encrypt));
            byte[] loginMsg = new byte[encrypt.length + 4];
            System.arraycopy(TCPUtil.getBytes(encrypt.length), 0, loginMsg, 0, 4);
            System.arraycopy(encrypt, 0, loginMsg, 4, encrypt.length);
            return frmConfig(loginMsg, seq, MessageType.LOGIN);
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private TCPMessage frmConfig(String msg, int seq, MessageType type) {
        return frmConfig(msg.getBytes(), seq, type);
    }

    private TCPMessage frmConfig(byte[] msg, int seq, MessageType type) {
        String username = conCfg.getProperty("sender.username");
        int userNo = Integer.parseInt(conCfg.getProperty("sender.user.number"));
        logger.debug(String.format("Username: %s, UserNo: %d", username, userNo));
        return new TCPMessage(msg, seq, username, userNo, TCPUtil.timeNow(), type.getType());
    }
}
