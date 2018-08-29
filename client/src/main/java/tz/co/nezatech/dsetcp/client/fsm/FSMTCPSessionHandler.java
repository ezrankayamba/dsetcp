package tz.co.nezatech.dsetcp.client.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.message.MarketDataType;
import tz.co.nezatech.dsetp.util.message.MessageType;

import java.io.OutputStream;
import java.time.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FSMTCPSessionHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(FSMTCPSessionHandler.class);
    private final FSMSessionEventListener listener;
    private OutputStream output;

    public FSMTCPSessionHandler(FSMSessionEventListener listener, OutputStream output) {
        this.listener = listener;
        this.output = output;
        System.out.println("New session established... :)");
    }

    public void sessionEstablished() throws Exception {
        heartBeat();
        marketClosing();
        marketClosed();
        initMessages();
    }

    private void initMessages() throws Exception {
        byte[] date = TCPUtil.dateNow(0);

        byte[] startOfDayDownload = new byte[24];

        startOfDayDownload[1] = (byte) 0; // unused
        startOfDayDownload[2] = (byte) 0; // last piece of chunk
        startOfDayDownload[3] = (byte) 0; //re-request
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 4, 4); // action
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 8, 4); // specific record
        System.arraycopy(date, 0, startOfDayDownload, 12, date.length); // date

        startOfDayDownload[0] = MarketDataType.MARKET_DISPLAY_DATA.getType(); // data type
        FSMTCPWriter.sendMessage(startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);

        //startOfDayDownload[0] = MarketDataType.INDICES.getType(); // data type
        //FSMTCPWriter.sendMessage(startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);

        //startOfDayDownload[0] = MarketDataType.INSTRUMENTS_DATA.getType(); // data type
        //FSMTCPWriter.sendMessage(startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);

        FSMTCPWriter.sendMessage(new byte[]{(byte) 0}, MessageType.REQUEST_SCREEN_OPEN, output);

        //Request Market Data
        List<String> contracts = ClientFSM.getContracts();
        byte[] msg99 = new byte[(contracts.size() * 48) + 2];
        short qty = (short) contracts.size();
        byte[] qtyBytes = TCPUtil.shortToBytes(qty);

        msg99[0] = qtyBytes[1];
        msg99[1] = qtyBytes[0];
        AtomicInteger pointer = new AtomicInteger(2);
        //msg99[++pointer] = (byte) c.length();
        contracts.forEach(c -> {
            byte[] contract = TCPUtil.contract(c);
            LOGGER.debug("Contract Length: " + contract.length + ", " + c);
            System.arraycopy(contract, 0, msg99, pointer.get(), contract.length);
            pointer.addAndGet(48);
        });
        FSMTCPWriter.sendMessage(msg99, MessageType.FUTURE_CONTRACT_SUBSCRIPTION, output);
    }

    private void marketClosing() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        String timeStr = ClientFSM.getConfig().getProperty("config.market.closing.time", "15:05:00");
        LocalTime ct = LocalTime.parse(timeStr);
        LOGGER.debug("Trigger on market close");
        scheduler.scheduleAtFixedRate(() -> {
                    if (marketClosed) {
                        LOGGER.error(">> Market is already closed, no closing messages will be sent");
                        scheduler.shutdownNow();
                        return;
                    }
                    LOGGER.debug("Market is closing, requesting MTM Data");
                    byte[] date = TCPUtil.dateNow(0);
                    byte[] startOfDayDownload = new byte[24];
                    startOfDayDownload[1] = (byte) 0; // unused
                    startOfDayDownload[2] = (byte) 0; // last piece of chunk
                    startOfDayDownload[3] = (byte) 0; //re-request
                    System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 4, 4); // action
                    System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 8, 4); // specific record
                    System.arraycopy(date, 0, startOfDayDownload, 12, date.length); // date

                    startOfDayDownload[0] = MarketDataType.MTM_DATA.getType(); // data type
                    try {
                        FSMTCPWriter.sendMessage(startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);
                        listener.marketClosing();
                        scheduler.shutdownNow();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, delay(ct.getHour(), ct.getMinute(), ct.getSecond()),
                24 * 60 * 60, TimeUnit.SECONDS);
    }

    void marketClosed() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        String timeStr = ClientFSM.getConfig().getProperty("config.market.closed.time", "15:30:00");
        LocalTime ct = LocalTime.parse(timeStr);
        LOGGER.debug("Trigger on market close");
        scheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (marketClosed) {
                            LOGGER.error(">> Market is already closed, probably due to TCP Session problems");
                            scheduler.shutdownNow();
                            return;
                        }
                        LOGGER.debug("Market is closing, Log out");
                        FSMTCPWriter.sendMessage("".getBytes(), MessageType.LOG_OUT, output);
                        marketClosed = true;
                        listener.marketClosed();
                        scheduler.shutdownNow();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, delay(ct.getHour(), ct.getMinute(), ct.getSecond()),
                24 * 60 * 60, TimeUnit.SECONDS);
    }


    private boolean marketClosed = false;

    private void heartBeat() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        LOGGER.debug("Enable heartbeat");
        final Runnable hb = () -> {
            try {
                if (marketClosed) {
                    scheduler.shutdownNow();
                    LOGGER.error(">> Market is closed, no heartbeat will be sent");
                    return;
                }
                LOGGER.debug(">> Sending heartbeat");
                FSMTCPWriter.sendMessage("".getBytes(), MessageType.HEART_BEAT_CLT, output);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("socket write error")) {
                    LOGGER.error(">> Market TCP session is NOT OK, app is disconnected");
                    scheduler.shutdownNow();
                    listener.disconnected();
                    return;
                }
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(hb, 30, 30, TimeUnit.SECONDS);
    }

    private Long delay(int withHour, int withMin, int withSec) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Africa/Dar_es_Salaam");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext;
        zonedNext = zonedNow.withHour(withHour).withMinute(withMin).withSecond(withSec);
        if (zonedNow.compareTo(zonedNext) > 0) zonedNext = zonedNext.plusDays(1);
        Duration duration = Duration.between(zonedNow, zonedNext);
        System.out.println(String.format("%d, %d, %d => %d", withHour, withMin, withSec, duration.getSeconds() / 60));
        return duration.getSeconds();
    }

    interface FSMSessionEventListener {
        void marketClosing();

        void marketClosed();

        void disconnected();
    }
}