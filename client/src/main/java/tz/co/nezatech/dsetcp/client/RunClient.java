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
import java.io.PrintWriter;
import java.net.Socket;

public class RunClient extends MessageReader {
    Logger logger = LoggerFactory.getLogger(RunClient.class.getName());
    Config conCfg = ConnectionConfig.get("client.cfg");
    short appState = 0;
    int msgSequence = 0;

    public static void main(String[] args) {
        RunClient client = new RunClient();
        client.init();
    }

    void init() {
        logger.debug("Starting TCP/IP connection");
        String ip = conCfg.getProperty("tcp.server.host");
        String port = conCfg.getProperty("tcp.server.port");
        String compId = conCfg.getProperty("sender.comp.id");
        String pwd = conCfg.getProperty("sender.password");
        logger.debug(String.format("Server: %s:%s ", ip, port));
        logger.debug(String.format("Client: %s ", compId));

        try {
            try (Socket con = new Socket(ip, Integer.parseInt(port))) {
                logger.debug(String.format("Successfully connected to server. IP: %s, Port: %s. ", ip, port));
                OutputStream output = con.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                while (true) {
                    InputStream input = con.getInputStream();
                    int avail = input.available();
                    if (avail != 0) {
                        logger.debug("Some data to read: " + avail);
                        readMessage("0", input, output);
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
        TCPUtil.print(mh);
        logger.debug(String.format("Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
        logger.debug("Message: " + new String(msg));

        if (appState == 0) {//Just connected
            MessageType type = MessageType.byType(mh[28]);
            logger.info("MessageType: " + type);
            if (type == MessageType.ACK_SUCCESS) {
                appState = 1;

                //send login message
                this.msgSequence++;
                try {
                    sendLogin(msg, output);
                } catch (IOException e) {
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
        }
    }

    private void sendLogin(byte[] msg, OutputStream output) throws IOException {
        TCPMessage login = login(msgSequence, msg);
        output.write(login.getMessage());
        output.flush();
    }

    private TCPMessage login(int seq, byte[] key) {
        try {
            String keyStr = new String(key);
            logger.debug("Key: " + keyStr);
            String pwd = conCfg.getProperty("sender.pasword");
            byte[] encrypt = RSAUtil.encryptFromXmlKey(pwd, new String(key));
            logger.debug(pwd + ": " + encrypt);
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
        return new TCPMessage(msg, seq, conCfg.getProperty("sender.username"), conCfg.getProperty("sender.user.id"), TCPUtil.timeNow(), type.getType());
    }

}
