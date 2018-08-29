package tz.co.nezatech.dsetcp.client.fsm;

import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.message.MessageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FSMTCPReader {

    public static InMessage read(InputStream input) throws IOException {
        InMessage message = new InMessage();
        byte[] th = new byte[4];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        if ((count = input.read(th)) > 0) {
            baos.write(th);
            message.setTransHeader(th);
            short len = TCPUtil.getMsgLength(th);
            byte[] mh = new byte[29];
            message.setMsgHeader(mh);
            if ((count = input.read(mh)) > 0) {
                baos.write(mh);
                MessageType type = MessageType.byType(mh[28]);
                message.setType(type);
                byte[] msg = new byte[len - 29];
                if ((count = input.read(msg)) > 0) {
                    baos.write(msg);
                    message.setBody(msg);
                }
            }
        }
        System.out.println("Bytes read: " + count);
        message.setComplete(baos.toByteArray());
        return message;
    }
}
