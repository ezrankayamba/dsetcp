package tz.co.nezatech.dsetp.util.message;

public enum MarketDataType {
    MARKET_DISPLAY_DATA((byte) 1),
    INSTRUMENTS_DATA((byte) 2),
    CONTRACT_DATES((byte) 3),
    STRIKE_DATA((byte) 4),
    MEMBER_DATA((byte) 15),
    MTM_DATA((byte) 16),
    HOLIDAY((byte) 18),
    CLEARING_MEMBER_DATA((byte) 65),
    EXCHANGE_ANNOUNCEMENTS((byte) 89),
    NEWS((byte) 129),
    COUPON_INFORMATION((byte) 131),
    TRADING_SESSIONS((byte) 136),
    INDICES((byte) 138),
    INDICES_DATA((byte) 139),
    DATA_TYPE_UNKNOWN((byte) 255);

    private byte type;

    MarketDataType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public static MarketDataType byType(byte type) {
        for (MarketDataType b : MarketDataType.values()) {
            if (b.type == type) {
                return b;
            }
        }
        MarketDataType tmp = MarketDataType.DATA_TYPE_UNKNOWN;
        tmp.setType(type);
        return tmp;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + type + ")";
    }
}
