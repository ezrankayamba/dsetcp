package tz.co.nezatech.dsetcp.client;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.RSAUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.message.MarketDataType;
import tz.co.nezatech.dsetp.util.message.MessageReader;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;


public class RunClient extends MessageReader {
    private Logger logger = LoggerFactory.getLogger(RunClient.class.getName());
    Config conCfg;


    short appState = 0;
    //int msgSequence = 0;
    private final String ip;
    private final String port;

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

        client.dbSetup();
        client.init();
    }

    private void dbSetup() {
        try {
            Properties properties = new Properties();
            properties.load(RunClient.class.getClassLoader().getResourceAsStream("db.properties"));
            ConnectionPool.configure(properties);
        } catch (IOException e) {
            e.printStackTrace();
            String msg = "Exiting application, no db configurations found!";
            stopAppDueToFailure(msg);
        }
    }

    void stopAppDueToFailure(String msg) {
        System.err.println(msg);
        logger.error(msg);
        ConnectionPool.dataSource().close();
        System.exit(-1);
    }

    private void init() {
        logger.debug(String.format("Starting TCP/IP connection -> %s:%s", ip, port));
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

    void restartApp(int pause) {
        logger.debug("App will try to re-initiate this connection in " + pause + " seconds");
        pause(pause);
        init();
    }

    @Override
    public void process(byte[] fullMsg, String id, byte[] mh, byte[] msg, OutputStream output) {
        if (msg == null) {
            logger.debug("<< Empty message, could be a heartbeat");
            return;
        }
        int seqNo = TCPUtil.seqNo(mh);
        String username = TCPUtil.getUsername(mh).trim();
        String userNo = TCPUtil.getUserNo(mh).trim();
        String time = TCPUtil.getTime(mh);
        logger.debug(String.format("<< Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
        logger.debug("<< Message(HEX): " + TCPUtil.text(msg));
        MessageType type = MessageType.byType(mh[28]);
        logger.debug("App FSMState: " + appState);
        logger.debug("<< MessageType: " + type);
        ClientMessageProcessor.process(this, type, fullMsg, msg, output);
    }

    void sendLogin(byte[] msg, OutputStream output) {
        TCPMessage login = login(msg);
        assert login != null;
        ClientMessageProcessor.sendMessage(login, MessageType.LOGIN, output);
    }

    void enableHeartbeat(OutputStream output) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        logger.debug("Market is open");
        final Runnable hb = () -> {
            logger.info(">> Sending heartbeat");
            ClientMessageProcessor.sendMessage(conCfg, "".getBytes(), MessageType.HEART_BEAT_CLT, output);
        };
        scheduler.scheduleAtFixedRate(hb, 60, 60, SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
                    logger.debug("Market is closing, requesting MTM Data");
                    byte[] date = TCPUtil.dateNow(0);

                    byte[] startOfDayDownload = new byte[24];

                    startOfDayDownload[1] = (byte) 0; // unused
                    startOfDayDownload[2] = (byte) 0; // last piece of chunk
                    startOfDayDownload[3] = (byte) 0; //re-request
                    System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 4, 4); // action
                    System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 8, 4); // specific record
                    System.arraycopy(date, 0, startOfDayDownload, 12, date.length); // date

                    startOfDayDownload[0] = MarketDataType.MTM_DATA.getType(); // data type
                    ClientMessageProcessor.sendMessage(conCfg, startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);
                }, delay(15, 05, 0),
                24 * 60 * 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
                    logger.debug("Market is closing, Log out");
                    ClientMessageProcessor.sendMessage(conCfg, "".getBytes(), MessageType.LOG_OUT, output);

                    //stop the scheduler for today
                    scheduler.shutdownNow();
                    appState = 99;
                    marketClosed();
                }, delay(15, 31, 0),
                24 * 60 * 60, TimeUnit.SECONDS);
    }

    private void marketClosed() {
        final ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
        scheduler2.scheduleAtFixedRate(() -> {
                    logger.debug("Market is open now");
                    appState = -1;
                    scheduler2.shutdownNow();
                    restartApp(60);
                }, delay(7, 0, 0),
                24 * 60 * 60, TimeUnit.SECONDS);
    }

    private Long delay(int withHour, int withMin, int withSec) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Africa/Dar_es_Salaam");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext;
        zonedNext = zonedNow.withHour(withHour).withMinute(withMin).withSecond(withSec);
        if (zonedNow.compareTo(zonedNext) > 0) zonedNext = zonedNext.plusDays(1);

        Duration duration = Duration.between(zonedNow, zonedNext);
        return duration.getSeconds();
    }

    private TCPMessage login(byte[] key) {
        try {
            logger.debug("Key");
            String pwd = conCfg.getProperty("sender.password");
            byte[] encrypt = RSAUtil.encryptFromXmlKey(pwd, new String(key));
            byte[] loginMsg = new byte[encrypt.length + 4];
            System.arraycopy(TCPUtil.getBytes(encrypt.length), 0, loginMsg, 0, 4);
            System.arraycopy(encrypt, 0, loginMsg, 4, encrypt.length);
            return ClientMessageProcessor.frmConfig(conCfg, loginMsg, MessageType.LOGIN);
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void process(ByteArrayOutputStream baos, OutputStream output) throws IOException {
        byte[] bytes = baos.toByteArray();
        String text = TCPUtil.text(bytes);
        MessageType type = MessageType.byType(bytes[(4 + 29 - 1)]);//the last byte of the message header
        try {
            HikariDataSource ds = ConnectionPool.dataSource();
            try (Connection con = ds.getConnection()) {
                logger.debug("Logging incoming message");
                PreparedStatement s = con.prepareStatement("INSERT INTO agm_message (msg_type, msg_direction, msg_details) VALUES (?, ?, ?)");
                s.setString(1, type.toString());
                s.setString(2, "FROM_SVR");
                s.setString(3, text);

                int records = s.executeUpdate();
                logger.debug(String.format("Logged %d message(s)", records));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        super.process(baos, output);
    }
}
