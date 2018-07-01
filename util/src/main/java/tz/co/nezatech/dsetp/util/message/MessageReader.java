package tz.co.nezatech.dsetp.util.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;

import java.io.IOException;
import java.io.InputStream;

public abstract class MessageReader implements MessageHandler {
    Logger logger = LoggerFactory.getLogger(MessageReader.class.getName());

    public void readMessage(String id, InputStream input) throws IOException {
        byte[] th = new byte[4];
        int count;
        if ((count = input.read(th)) > 0) {
            logger.debug("Read: " + count);
            short len = TCPUtil.getMsgLength(th);
            byte[] mh = new byte[29];
            if ((count = input.read(mh)) > 0) {
                logger.debug("Read: " + count);
                byte[] msg = new byte[len - 29];
                if ((count = input.read(msg)) > 0) {
                    logger.debug("Read: " + count);
                    process(id, mh, msg);
                }
            }
        }
    }
}
