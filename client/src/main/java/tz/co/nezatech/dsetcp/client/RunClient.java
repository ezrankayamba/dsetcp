package tz.co.nezatech.dsetcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.RSAUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.exception.TCPConnectionException;
import tz.co.nezatech.dsetp.util.message.MessageReader;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;


public class RunClient extends MessageReader {
    Logger logger = LoggerFactory.getLogger(RunClient.class.getName());
    Config conCfg;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    short appState = 0;
    int msgSequence = 0;
    final String ip;
    final String port;

    public RunClient() {
        super();
        String profile = System.getenv("profile");
        if (profile != null && !profile.trim().isEmpty() && profile.trim().equalsIgnoreCase("test")) {
            conCfg = ConnectionConfig.get("client-test.cfg");
        } else {
            conCfg = ConnectionConfig.get("client.cfg");
        }
        ip = conCfg.getProperty("tcp.server.host");
        port = conCfg.getProperty("tcp.server.port");
    }

    public static void main(String[] args) {
        RunClient client = new RunClient();
        client.init();
    }

    void init() {
        logger.debug("Starting TCP/IP connection");

        try {
            try (final Socket con = new Socket(ip, Integer.parseInt(port))) {
                logger.debug(String.format("Successfully connected to server. IP: %s, Port: %s. ", ip, port));
                while (true) {
                    try {
                        InputStream input = con.getInputStream();
                        int avail = input.available();
                        if (avail != 0) {
                            logger.debug("Some data to read: " + avail);
                            readMessage("0", input, con.getOutputStream());
                        }
                    } catch (Exception e) {
                        logger.error("Exception(main): " + e.getMessage());
                        e.printStackTrace();
                        int pause = 60;
                        restartApp(pause);
                        break;
                    }
                    pause(2);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            restartApp(60);
        }
    }

    private void restartApp(int pause) {
        logger.debug("App will try to re-initiate this connection in " + pause + " seconds");
        pause(pause);
        init();
    }

    @Override
    public void process(String id, byte[] mh, byte[] msg, OutputStream output) throws IOException {
        if (msg == null) {
            logger.debug("<< Empty message, could be a heartbeat");
            return;
        }
        int seqNo = TCPUtil.seqNo(mh);
        String username = TCPUtil.getUsername(mh).trim();
        String userNo = TCPUtil.getUserNo(mh).trim();
        String time = TCPUtil.getTime(mh);
        logger.debug(String.format("<< Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
        //logger.debug("<< Message: " + new String(msg));
        logger.debug("<< Header(HEX): " + TCPUtil.text(mh));
        logger.debug("<< Message(HEX): " + TCPUtil.text(msg));
        MessageType type = MessageType.byType(mh[28]);
        logger.debug("App State: " + appState);
        logger.debug("<< MessageType: " + type);
        if (appState == 0) {//Just connected
            if (type == MessageType.DAILY_KEY) {
                appState = 1;
                //send login message
                this.msgSequence++;
                sendLogin(msg, output);
                logger.debug(">> Login message sent");
            } else {
                logger.error("Failed to connect to DSE. Check logs or contact support");
                System.exit(-1);
            }
        } else if (appState == 1) {//Login response
            logger.debug("<< Login response: " + new String(msg));
            if (type == MessageType.ACK_SUCCESS) {
                logger.debug("<< Login success");
                appState = 2;
                onSuccessLogin(output);
            } else if (type == MessageType.ACK_ERROR) {
                logger.error("<< Login failed: " + new String(msg));
                appState = -1;//Login fail
                restartApp(60);
            } else {
                logger.debug("<< Login status unknown: " + type);
            }
        } else {
            logger.debug("<< Application message: " + type + " = " + TCPUtil.text(msg));
        }
    }

    private void onSuccessLogin(OutputStream output) {
        enableHeartbeat(String.format("%s:%s", ip, port), output);

        LocalDateTime now = LocalDateTime.now();
        int day = now.getDayOfMonth();
        int month = now.getMonthValue();
        int year = now.getYear();

        byte[] startOfDayDownload = new byte[24];
        startOfDayDownload[0] = (byte) 1; // data type
        startOfDayDownload[1] = (byte) 0; // unused
        startOfDayDownload[2] = (byte) 0; // last piece of chunk
        startOfDayDownload[3] = (byte) 0; //re-request
        System.arraycopy(TCPUtil.intToBytes(0), 0, startOfDayDownload, 4, 4); // action
        System.arraycopy(TCPUtil.intToBytes(0), 0, startOfDayDownload, 8, 4); // specific record
        System.arraycopy(TCPUtil.intToBytes(year), 0, startOfDayDownload, 12, 4); // year
        System.arraycopy(TCPUtil.intToBytes(month), 0, startOfDayDownload, 16, 4); // month
        System.arraycopy(TCPUtil.intToBytes(day), 0, startOfDayDownload, 20, 4); // day
        sendMessage(startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);
        sendMessage(new byte[]{(byte) 0}, MessageType.REQUEST_SCREEN_OPEN, output);

        //Request Market Data
        List<String> contracts = List.of(conCfg.getProperty("sender.contract.name"));
        byte[] msg99 = new byte[(contracts.size() * 48) + 2];
        short qty = (short) contracts.size();
        byte[] qtyBytes = TCPUtil.shortToBytes(qty);

        msg99[0] = qtyBytes[1];
        msg99[1] = qtyBytes[0];
        AtomicInteger pointer = new AtomicInteger(2);
        //msg99[++pointer] = (byte) c.length();
        contracts.forEach(c -> {
            byte[] contract = TCPUtil.contract(c);
            logger.debug("Contract Length: " + contract.length + ", " + c);
            System.arraycopy(contract, 0, msg99, pointer.get(), contract.length);
            pointer.addAndGet(48);
        });
        sendMessage(msg99, MessageType.FUTURE_CONTRACT_SUBSCRIPTION, output);
    }

    private void sendLogin(byte[] msg, OutputStream output) throws IOException {
        TCPMessage login = login(msgSequence, msg);
        byte[] msgToSend = login.getMessage();
        logger.debug(String.format(">> Login Message: %s ", TCPUtil.text(msgToSend)));
        output.write(msgToSend);
        output.flush();
    }

    public void enableHeartbeat(String id, OutputStream output) {
        final Runnable hb = () -> {
            logger.info(">> Sending heartbeat");
            sendMessage("", MessageType.HEART_BEAT_CLT, output);
        };
        scheduler.scheduleAtFixedRate(hb, 60, 60, SECONDS);
    }

    private void sendMessage(String msg, MessageType type, OutputStream output) {
        sendMessage(msg.getBytes(), type, output);
    }

    private void sendMessage(byte[] msg, MessageType type, OutputStream output) {
        TCPMessage outMsg = frmConfig(msg, ++msgSequence, type);
        //logger.debug(">> Sending message: " + type + " = " + new String(outMsg.getMessage()));
        logger.debug(">> Sending message(HEX): " + type + " = " + TCPUtil.text(outMsg.getMessage()));
        try {
            output.write(outMsg.getMessage());
            output.flush();
        } catch (IOException e) {
            logger.error("Failed to send msg: " + e.getMessage());
            e.printStackTrace();
            try {
                throw new TCPConnectionException(e.getMessage());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private TCPMessage login(int seq, byte[] key) {
        try {
            String keyStr = new String(key);
            logger.debug("<< Key: " + keyStr);
            String pwd = conCfg.getProperty("sender.password");
            //logger.debug("Plain pwd: " + pwd);
            byte[] encrypt = RSAUtil.encryptFromXmlKey(pwd, new String(key));
            //logger.debug("Encrypted pwd: " + new String(encrypt));
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

    private TCPMessage frmConfig(byte[] msg, int seq, MessageType type) {
        String username = conCfg.getProperty("sender.username");
        int userNo = Integer.parseInt(conCfg.getProperty("sender.user.number"));
        logger.debug(String.format("Username: %s, UserNo: %d", username, userNo));
        return new TCPMessage(msg, seq, username, userNo, TCPUtil.timeNow(), type.getType());
    }
}
