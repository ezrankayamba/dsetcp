package tz.co.nezatech.dsetp.util.message;

public enum  MessageType {
    DAILY_KEY((byte) 16),
    MARKET_DISPLAY((byte) 36),
    LOGIN((byte) 0),
    HEART_BEAT_SVR((byte) 10),
    HEART_BEAT_CLT((byte) 84),
    ACK_SUCCESS((byte) 1),
    ACK_ERROR((byte) 125);

    private byte type;

    MessageType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public static MessageType byType(byte type) {
        for (MessageType b : MessageType.values()) {
            if (b.type == type) {
                return b;
            }
        }
        return null;
    }
}
