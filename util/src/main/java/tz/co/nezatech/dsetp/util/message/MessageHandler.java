package tz.co.nezatech.dsetp.util.message;

import java.io.OutputStream;

public interface MessageHandler {
    public void process(String id, byte[] mh, byte[] msg, OutputStream output);

    public default byte[] encrypt() {
        byte[] encypted = null;

        return encypted;
    }
}
