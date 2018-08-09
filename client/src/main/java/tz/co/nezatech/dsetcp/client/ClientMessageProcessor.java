package tz.co.nezatech.dsetcp.client;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.exception.TCPConnectionException;
import tz.co.nezatech.dsetp.util.message.*;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientMessageProcessor {
    private static Logger logger = LoggerFactory.getLogger(ClientMessageProcessor.class);

    public static TCPMessage frmConfig(RunClient client, byte[] msg, int seq, MessageType type) {
        String username = client.conCfg.getProperty("sender.username");
        int userNo = Integer.parseInt(client.conCfg.getProperty("sender.user.number"));
        return new TCPMessage(msg, seq, username, userNo, TCPUtil.timeNow(), type.getType());
    }

    public static void sendMessage(RunClient client, String msg, MessageType type, OutputStream output) {
        sendMessage(client, msg.getBytes(), type, output);
    }

    public static void sendMessage(RunClient client, byte[] msg, MessageType type, OutputStream output) {
        TCPMessage outMsg = frmConfig(client, msg, client.msgSequence++, type);
        ClientMessageProcessor.sendMessage(outMsg, type, output);
    }

    public static void sendMessage(TCPMessage outMsg, MessageType type, OutputStream output) {
        String text = TCPUtil.text(outMsg.getMessage());
        logger.debug(">> Sending message(HEX): " + type + " = " + text);
        try {
            HikariDataSource ds = ConnectionPool.dataSource();
            try (Connection con = ds.getConnection()) {
                logger.debug("Logging incoming message");
                PreparedStatement s = con.prepareStatement("INSERT INTO agm_message (msg_type, msg_direction, msg_details) VALUES (?, ?, ?)");
                s.setString(1, type.toString());
                s.setString(2, "TO_SVR");
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

    public static void onSuccessLogin(RunClient client, OutputStream output) {
        client.enableHeartbeat("0", output);
        byte[] date = TCPUtil.dateNow(0);

        byte[] startOfDayDownload = new byte[24];

        startOfDayDownload[1] = (byte) 0; // unused
        startOfDayDownload[2] = (byte) 0; // last piece of chunk
        startOfDayDownload[3] = (byte) 0; //re-request
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 4, 4); // action
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 8, 4); // specific record
        System.arraycopy(date, 0, startOfDayDownload, 12, date.length); // date
        startOfDayDownload[0] = MarketDataType.INSTRUMENTS_DATA.getType(); // data type
        sendMessage(client, startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);
        sendMessage(client, new byte[]{(byte) 0}, MessageType.REQUEST_SCREEN_OPEN, output);

        //Request Market Data
        List<String> contracts = List.of(client.conCfg.getProperty("sender.contract.name").split(","));
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
        sendMessage(client, msg99, MessageType.FUTURE_CONTRACT_SUBSCRIPTION, output);
    }

    public static void process(RunClient client, MessageType type, byte[] fullMsg, byte[] msg, OutputStream output) throws IOException {
        if (client.appState == 0) {//Just connected
            if (type == MessageType.DAILY_KEY) {
                client.appState = 1;
                //send login message
                client.msgSequence++;
                client.sendLogin(msg, output);
                logger.debug(">> Login message sent");
            } else {
                client.stopAppDueToFailure("Failed to connect to DSE. Check logs or contact support");
            }
        } else if (client.appState == 1) {//Login response
            logger.debug("<< Login response: " + new String(msg));
            if (type == MessageType.ACK_SUCCESS) {
                logger.debug("<< Login success");
                client.appState = 2;
                ClientMessageProcessor.onSuccessLogin(client, output);
            } else if (type == MessageType.ACK_ERROR) {
                logger.error("<< Login failed: " + new String(msg));
                client.appState = -1;//Login fail
                client.restartApp(60);
            } else {
                logger.debug("<< Login status unknown: " + type);
            }
        } else {
            if (type.equals(MessageType.START_OF_DAY_DOWNLOAD)) {
                logger.debug("Uncompress the msg(36) GZIP");
                try {
                    MarketDataType dataType = MarketDataType.byType(fullMsg[33]);// 34th byte
                    msg = StartOfDayDownload.decompress(fullMsg);
                    logger.debug("Decompressed " + MessageType.START_OF_DAY_DOWNLOAD + " :" + TCPUtil.text(msg));
                    StartOfDayDownload.read(dataType, msg);
                } catch (Exception e) {
                    logger.error("Failed to decode the message: " + type);
                }
            } else if (type.equals(MessageType.FUTURE_CONTRACT_SUBSCRIPTION)) {
                logger.debug("Uncompress the msg(36) GZIP");
                try {
                    msg = FutureContractSubscription.decompress(fullMsg);
                    logger.debug("Decompressed " + MessageType.FUTURE_CONTRACT_SUBSCRIPTION + " :" + TCPUtil.text(msg));
                    Map map = FutureContractSubscription.read(msg);
                    logger.debug("Params: " + map);
                } catch (Exception e) {
                    logger.error("Failed to decode the message: " + type);
                }
            } else if (type.equals(MessageType.DISPLAY_UPDATES)) {
                try {
                    logger.debug("New Update - " + MessageType.DISPLAY_UPDATES + " :" + TCPUtil.text(msg));
                    Map map = FutureContractSubscription.readDisplayUpdate(msg, true);
                    logger.debug("Params: " + map);
                } catch (Exception e) {
                    logger.error("Failed to decode the message: " + type);
                }
            } else if (type == MessageType.BUSINESS_REJECT) {
                int poiter = 32;
                int seqNo_ = TCPUtil.toInt(TCPUtil.extract(fullMsg, poiter, 4));
                poiter += 4;
                poiter += 3;// skip msg type ref

                int size = fullMsg[poiter++];
                String errorMsg = new String(TCPUtil.extract(fullMsg, poiter, size));
                logger.error("Business Reject Msg(" + seqNo_ + "): " + errorMsg);
            }
            logger.debug("<< Application message: " + type + " = " + TCPUtil.text(msg));
        }
    }
}
