package tz.co.nezatech.dsetcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                        readMessage("0", input);
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
    public void process(String id, byte[] mh, byte[] msg) {
        logger.debug("ID: " + id);
        int seqNo = TCPUtil.seqNo(mh);
        String username = TCPUtil.getUsername(mh).trim();
        String userNo = TCPUtil.getUserNo(mh).trim();
        String time = TCPUtil.getTime(mh);
        logger.debug(String.format("Message Header | SeqNo: %d, Username: %s, UserNo: %s, Time: %s", seqNo, username, userNo, time));
        logger.debug("Message: " + new String(msg));
    }

    private void pause(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TCPMessage frmConfig(String msg, int seq, MessageType type) {
        return new TCPMessage(msg.getBytes(), seq, conCfg.getProperty("sender.username"), conCfg.getProperty("sender.user.id"), TCPUtil.timeNow(), type.getType());
    }
}
