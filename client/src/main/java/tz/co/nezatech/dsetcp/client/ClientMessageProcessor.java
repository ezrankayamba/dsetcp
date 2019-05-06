package tz.co.nezatech.dsetcp.client;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
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

class ClientMessageProcessor {
    private static Logger logger = LoggerFactory.getLogger(ClientMessageProcessor.class);

    static TCPMessage frmConfig(Config conCfg, byte[] msg, MessageType type) {
        String username = conCfg.getProperty("sender.username");
        int userNo = Integer.parseInt(conCfg.getProperty("sender.user.number"));
        return new TCPMessage(msg, MsgSequencer.next(), username, userNo, TCPUtil.timeNow(), type.getType());
    }

    /*static void sendMessage(Config conCfg, String msg, MessageType type, OutputStream output) {
        sendMessage(conCfg, msg.getBytes(), type, output);
    }*/

    static void sendMessage(Config conCfg, byte[] msg, MessageType type, OutputStream output) {
        TCPMessage outMsg = frmConfig(conCfg, msg, type);
        ClientMessageProcessor.sendMessage(outMsg, type, output);
    }

    static void sendMessage(TCPMessage outMsg, MessageType type, OutputStream output) {
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

    private static void onSuccessLogin(Config conCfg, OutputStream output) {
        byte[] date = TCPUtil.dateNow(0);

        byte[] startOfDayDownload = new byte[24];

        startOfDayDownload[1] = (byte) 0; // unused
        startOfDayDownload[2] = (byte) 0; // last piece of chunk
        startOfDayDownload[3] = (byte) 0; //re-request
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 4, 4); // action
        System.arraycopy(TCPUtil.bytesEmpty(4), 0, startOfDayDownload, 8, 4); // specific record
        System.arraycopy(date, 0, startOfDayDownload, 12, date.length); // date

        startOfDayDownload[0] = MarketDataType.MARKET_DISPLAY_DATA.getType(); // data type
        sendMessage(conCfg, startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);

        startOfDayDownload[0] = MarketDataType.INDICES.getType(); // data type
        sendMessage(conCfg, startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);

        startOfDayDownload[0] = MarketDataType.INSTRUMENTS_DATA.getType(); // data type
        sendMessage(conCfg, startOfDayDownload, MessageType.START_OF_DAY_DOWNLOAD, output);


        sendMessage(conCfg, new byte[]{(byte) 0}, MessageType.REQUEST_SCREEN_OPEN, output);

        //Request Market Data
        List<String> contracts = List.of(conCfg.getProperty("sender.contract.name").split(","));
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
        sendMessage(conCfg, msg99, MessageType.FUTURE_CONTRACT_SUBSCRIPTION, output);
    }

    static void process(RunClient client, MessageType type, byte[] fullMsg, byte[] msg, OutputStream output) {
        if (client.appState == 0) {//Just connected
            if (type == MessageType.DAILY_KEY) {
                client.appState = 1;

                //send login message
                client.sendLogin(msg, output);
                logger.debug(">> Login message sent");
            } else {
                client.stopAppDueToFailure("Failed to connect to DSE. Check logs or contact support");
            }
        } else if (client.appState == 1) {//Login response
            logger.debug("<< Login response: " + new String(msg));
            String msgText = TCPUtil.text(msg);
            String invalidPwdText = TCPUtil.text("Invalid Password".getBytes());
            if (type == MessageType.ACK_SUCCESS && !msgText.contains(invalidPwdText)) {
                logger.debug("<< Login success");
                client.appState = 2;
                client.enableHeartbeat(output);
                ClientMessageProcessor.onSuccessLogin(client.conCfg, output);
            } else if (type == MessageType.ACK_ERROR || msgText.contains(invalidPwdText)) {
                logger.error("<< Login failed: " + new String(msg));
                client.appState = 0;//Login fail
                MsgSequencer.reset();
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
                try {
                    logger.debug("Reject Msg: " + TCPUtil.text(fullMsg));
                    int pointer = 33;
                    int seqNo_ = TCPUtil.toInt(TCPUtil.extract(fullMsg, pointer, 4), true);
                    pointer += 4;

                    int size = fullMsg[pointer++];
                    byte msgTypeBytes[] = new byte[]{0, 0, 0, 0};
                    byte[] extract = TCPUtil.extract(fullMsg, pointer, size);
                    for (int i = 0; i < 4; i++) {
                        if (i >= (4 - size)) {
                            msgTypeBytes[i] = extract[i - size];
                        }
                    }

                    int msgTypeInt = TCPUtil.toInt(msgTypeBytes);
                    MessageType messageType = MessageType.byType((byte) msgTypeInt);

                    size = fullMsg[pointer++];
                    String errorMsg = new String(TCPUtil.extract(fullMsg, pointer, size));
                    logger.error("Business Reject Msg(" + seqNo_ + ", " + messageType + "): " + errorMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logger.debug("<< Application message: " + type + " = " + TCPUtil.text(msg));
        }
    }
}
