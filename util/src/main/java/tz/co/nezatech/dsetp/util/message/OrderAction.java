package tz.co.nezatech.dsetp.util.message;

public enum OrderAction {
    BUY((byte) 'B'),
    SELL((byte) 'S'),
    NO_ACTION((byte) 'N');

    private byte type;

    OrderAction(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public static OrderAction byType(byte type) {
        for (OrderAction b : OrderAction.values()) {
            if (b.type == type) {
                return b;
            }
        }
        OrderAction tmp = OrderAction.NO_ACTION;
        tmp.setType(type);
        return tmp;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
