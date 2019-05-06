package tz.co.nezatech.dsetcp.client.fsm;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetcp.client.RunClient;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientFSM implements FSMTCPSessionHandler.FSMSessionEventListener {
    private static final String DATA_HOLIDAYS_FILE = "countWorkDays.txt";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientFSM.class);
    private FSMState state = FSMState.INIT_TCP;
    private boolean restart = false;
    private static Config config;
    public static String rsaCipher = "RSA";
    public static String rsaCipherKeyfactory = "RSA";

    static Config getConfig() {
        if (config == null)
            config = ConnectionConfig.get("client.cfg");
        return config;
    }

    public static void main(String[] args) {
        LOGGER.debug("FSM starting...");
        if (args.length >= 1) {
            rsaCipher = args[0];
            if (args.length >= 2) {
                rsaCipherKeyfactory = args[1];
            }
        }
        ClientFSM fsm = new ClientFSM();
        fsm.init();
    }

    public static List<String> getContracts() {
        List<String> contracts = List.of(getConfig().getProperty("sender.contract.name").split(","));
        return contracts;
    }

    public ClientFSM() {
        dbSetup();
        watchHolidayChanges();
        sessionHandler = new ThreadLocal<>();
    }

    private void dbSetup() {
        try {
            Properties properties = new Properties();
            properties.load(RunClient.class.getClassLoader().getResourceAsStream("db.properties"));
            ConnectionPool.configure(properties);
        } catch (IOException e) {
            e.printStackTrace();
            String msg = "Exiting application, no db configurations found!";
            LOGGER.error(msg);
            System.exit(-1);
        }
    }

    private void init() {
        init(false);
    }

    private void init(boolean restart) {
        state = FSMState.INIT_TCP;
        LOGGER.debug(restart ? "FSM Re-initializing" : "FSM Initializing");
        LOGGER.debug("State: " + state);
        Config conCfg = getConfig();

        String ip = conCfg.getProperty("tcp.server.host");
        int port = Integer.parseInt(conCfg.getProperty("tcp.server.port"));
        LOGGER.debug(String.format("Exchange server address >> %s:%d", ip, port));
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.debug("App is shutting down at state: " + state);
                if (state == FSMState.SESSION_ESTABLISHED && sessionHandler.get() != null) {
                    LOGGER.debug("Session is active, send logout message and disconnect");
                    sessionHandler.get().marketClosed();
                }
            }));


            if (marketIsOpen()) {
                LOGGER.debug("Market is already open");
                state = FSMState.MARKET_OPENED;
                startTCPConn(ip, port);
            } else {
                LOGGER.debug("Market is not open, will wait till it is open to start TCP Session");
                tillOpen();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Fatal failure happened!" + e.getMessage());
        }
    }

    private void tillOpen() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        String timeStr = ClientFSM.getConfig().getProperty("config.market.opened.time", "07:00:00");
        LocalTime ct = LocalTime.parse(timeStr);
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Africa/Dar_es_Salaam");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext;
        zonedNext = zonedNow.withHour(ct.getHour()).withMinute(ct.getMinute()).withSecond(ct.getSecond());
        if (zonedNow.compareTo(zonedNext) > 0) zonedNext = zonedNext.plusDays(1);
        Duration duration = Duration.between(zonedNow, zonedNext);
        LOGGER.debug("Will wait till market is open in => " + delayStr(duration.getSeconds()));
        scheduler.scheduleAtFixedRate(() -> {
            init(true);
        }, duration.getSeconds(), 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private void startTCPConn(String ip, int port) {
        restart = false;
        try (final Socket con = new Socket(ip, port)) {
            LOGGER.debug(String.format("Successfully connected to server. IP: %s, Port: %s. ", ip, port));
            state = FSMState.TCP_CONNECTED;
            while (!restart) {
                try {
                    InputStream input = con.getInputStream();
                    int avail = input.available();
                    if (avail != 0) {
                        LOGGER.debug("Some data to read: " + avail);
                        messageFrmServer(input, con.getOutputStream());
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception(main): " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
                pause(2);
            }
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                LOGGER.error("There is no connection to DSE server");
            } else {
                e.printStackTrace();
            }
        }

        LOGGER.debug("Session closed, will restart in 1 minute");
        pause(60);
        init(true);
    }

    private String delayStr(long seconds) {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);
        String dly = String.format("Days: %d, Hours: %d, Minutes: %d, Seconds: %d", day, hours, minute, second);
        return dly;
    }

    final ThreadLocal<FSMTCPSessionHandler> sessionHandler;

    private void messageFrmServer(InputStream input, OutputStream outputStream) throws Exception {
        InMessage read = FSMTCPReader.read(input);
        HikariDataSource ds = ConnectionPool.dataSource();
        try (Connection con = ds.getConnection()) {
            LOGGER.debug("<< Logging incoming message");
            PreparedStatement s = con.prepareStatement("INSERT INTO agm_message (msg_type, msg_direction, msg_details) VALUES (?, ?, ?)");
            s.setString(1, read.getType().toString());
            s.setString(2, "FROM_SVR");
            s.setString(3, TCPUtil.text(read.getComplete()));

            int records = s.executeUpdate();
            LOGGER.debug(String.format("Logged %d message(s)", records));
        }
        state = FSMProcessor.process(state, read, outputStream, output -> {
            LOGGER.debug("Session established");
            sessionHandler.set(new FSMTCPSessionHandler(this, output));
            sessionHandler.get().sessionEstablished();
        });
    }

    private boolean marketIsOpen() {
        String openTime = getConfig().getProperty("config.market.opened.time", "07:00:00");
        String closeTime = getConfig().getProperty("config.market.closed.time", "07:00:00");
        LocalTime open = LocalTime.parse(openTime);
        LocalTime close = LocalTime.parse(closeTime);
        LocalTime now = LocalTime.now();
        return now.isAfter(open) && now.isBefore(close);
    }

    private void pause(int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (Exception ignored) {
        }
    }

    private void watchHolidayChanges() {
        final Path dataPath = Paths.get(System.getProperty("user.dir") + File.separator + "data");
        TCPUtil.updateHolidays(dataPath + File.separator + DATA_HOLIDAYS_FILE);
        new Thread(() -> {
            System.out.println("Path: " + dataPath.toString());
            try {

                do {
                    WatchService watcher = dataPath.getFileSystem().newWatchService();
                    dataPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                    System.out.println("Watching: " + dataPath);
                    WatchKey watckKey = watcher.take();
                    watckKey.pollEvents().forEach(watchEvent -> {
                        if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            String tmp = watchEvent.context().toString();
                            System.out.println("Modify: " + tmp);
                            if (tmp.equalsIgnoreCase(DATA_HOLIDAYS_FILE)) {
                                List<LocalDate> localDates = TCPUtil.updateHolidays(dataPath + File.separator + DATA_HOLIDAYS_FILE);
                                System.out.println(localDates);
                            }
                        }
                    });
                } while (true);
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
            }
        }).start();
    }

    @Override
    public void marketClosing() {
        state = FSMState.MARKET_CLOSING;
        LOGGER.debug("Market closing...");
    }

    @Override
    public void marketClosed() {
        state = FSMState.MARKET_CLOSED;
        LOGGER.debug("Market closed, App will restart in 1 min");
        new Thread(() -> {
            pause(60);
            init(true);
        }).start();
    }

    @Override
    public void disconnected() {
        LOGGER.error("Market disconnected, App will restart in 1 min");
        new Thread(() -> {
            pause(20);
            init(true);
        }).start();
    }

    interface SessionEstablishedListener {
        void sessionEstablished(OutputStream output) throws Exception;
    }
}
