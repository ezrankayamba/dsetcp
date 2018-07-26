package tz.co.nezatech.dsetp.util.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface MessageHandler {
    public void process(String id, byte[] mh, byte[] msg, OutputStream output) throws IOException;
    public void process(ByteArrayOutputStream baos, OutputStream output) throws IOException;

    public default byte[] encrypt() {
        byte[] encypted = null;
        return encypted;
    }
}
