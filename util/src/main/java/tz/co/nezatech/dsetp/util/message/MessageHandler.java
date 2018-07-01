package tz.co.nezatech.dsetp.util.message;

public interface MessageHandler {
    public void process(String id, byte[] mh, byte[] msg);
}
