package tz.co.nezatech.dsetcp.client.fsm;

public enum FSMState {
    INIT_TCP(0),
    MARKET_OPENED(1),
    TCP_CONNECTED(2),
    LOGIN_SENT(3),
    SESSION_ESTABLISHED(4),
    MARKET_CLOSING(5),
    MARKET_CLOSED(6);
    int num;

    FSMState(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return String.format("%s [%d]", super.toString(), num);
    }
}
