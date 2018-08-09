package tz.co.nezatech.dsetp.util.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class MessageReader implements MessageHandler {
    Logger logger = LoggerFactory.getLogger(MessageReader.class.getName());

    public void readMessage(String id, InputStream input, OutputStream output) throws IOException {
        byte[] th = new byte[4];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        if ((count = input.read(th)) > 0) {
            baos.write(th);
            short len = TCPUtil.getMsgLength(th);
            byte[] mh = new byte[29];
            if ((count = input.read(mh)) > 0) {
                baos.write(mh);
                MessageType type = MessageType.byType(mh[28]);
                logger.debug("<< MessageType: " + type);
                byte[] msg = new byte[len - 29];
                if ((count = input.read(msg)) > 0) {
                    logger.debug("Msg Size: " + count);
                    baos.write(msg);
                    process(baos, output);
                } else {
                    process(baos, output);
                }
            }
        }
    }

    @Override
    public void process(ByteArrayOutputStream baos, OutputStream output) throws IOException {
        byte[] bytes = baos.toByteArray();
        logger.debug("<< Message: " + TCPUtil.text(bytes));
        byte[] th = new byte[4];
        System.arraycopy(bytes, 0, th, 0, 4);
        short len = TCPUtil.getMsgLength(th);
        byte[] mh = new byte[29];
        System.arraycopy(bytes, 4, mh, 0, 29);
        MessageType type = MessageType.byType(mh[28]);
        logger.debug("<< MessageType: " + type);
        int mlen = len - 29;
        if (mlen > 0) {
            byte[] msg = new byte[mlen];
            System.arraycopy(bytes, (4 + 29), msg, 0, mlen);
            process(bytes, "NewProcess", mh, msg, output);
        } else {
            logger.debug("No payload!");
            process(bytes, "NewProcessNoPayload", mh, null, output);
        }
    }

    public void pause(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
