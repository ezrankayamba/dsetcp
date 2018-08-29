package tz.co.nezatech.dsetcp.client.fsm;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetcp.client.MsgSequencer;
import tz.co.nezatech.dsetp.util.RSAUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.config.Config;
import tz.co.nezatech.dsetp.util.config.ConnectionConfig;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.TCPMessage;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FSMTCPWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSMTCPWriter.class);

    public static void sendLogin(InMessage message, OutputStream output) throws Exception {
        Config cfg = ClientFSM.getConfig();
        String pwd = cfg.getProperty("sender.password");
        String username = cfg.getProperty("sender.username");
        int userNo = Integer.parseInt(cfg.getProperty("sender.user.number"));
        TCPMessage login = login(message.getBody(), username, pwd, userNo);
        sendMessage(login, MessageType.LOGIN, output);
    }

    private static void sendMessage(TCPMessage outMsg, MessageType type, OutputStream output) throws Exception {
        String text = TCPUtil.text(outMsg.getMessage());
        LOGGER.debug(">> Sending message(HEX): " + type + " = " + text);
        HikariDataSource ds = ConnectionPool.dataSource();
        try (Connection con = ds.getConnection()) {
            LOGGER.debug(">> Logging outgoing message");
            PreparedStatement s = con.prepareStatement("INSERT INTO agm_message (msg_type, msg_direction, msg_details) VALUES (?, ?, ?)");
            s.setString(1, type.toString());
            s.setString(2, "TO_SVR");
            s.setString(3, text);

            int records = s.executeUpdate();
            LOGGER.debug(String.format(">> Logged %d message(s)", records));

            output.write(outMsg.getMessage());
            output.flush();

            LOGGER.debug(">> Message Sent: " + type);
        }
    }

    public static void sendMessage(byte[] outMsg, MessageType type, OutputStream output) throws Exception {
        Config cfg = ClientFSM.getConfig();
        String username = cfg.getProperty("sender.username");
        int userNo = Integer.parseInt(cfg.getProperty("sender.user.number"));
        sendMessage(getMesage(outMsg, username, userNo, type), type, output);
    }

    private static TCPMessage login(byte[] key, String username, String pwd, int userNo) throws Exception {
        byte[] encrypt = RSAUtil.encryptFromXmlKey(pwd, new String(key));
        byte[] loginMsg = new byte[encrypt.length + 4];
        System.arraycopy(TCPUtil.getBytes(encrypt.length), 0, loginMsg, 0, 4);
        System.arraycopy(encrypt, 0, loginMsg, 4, encrypt.length);
        //return new TCPMessage(loginMsg, MsgSequencer.next(), username, userNo, TCPUtil.timeNow(), MessageType.LOGIN.getType());
        return getMesage(loginMsg, username, userNo, MessageType.LOGIN);
    }

    private static TCPMessage getMesage(byte[] msg, String username, int userNo, MessageType type) throws Exception {
        return new TCPMessage(msg, MsgSequencer.next(), username, userNo, TCPUtil.timeNow(), type.getType());
    }

}
