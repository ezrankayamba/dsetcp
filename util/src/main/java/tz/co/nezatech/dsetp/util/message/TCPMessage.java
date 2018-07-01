package tz.co.nezatech.dsetp.util.message;

import tz.co.nezatech.dsetp.util.TCPUtil;

public class TCPMessage {
    /**
     * Transport header - 4 bytes
     */
    private byte[] th;
    /**
     * Message header - 29 bytes
     */
    private byte[] mh;
    /**
     * The message payload - n bytes as indicated on transport header
     */
    private byte[] payload;

    public TCPMessage(byte[] payload, int seqNo, String username, String userNo, byte[] time, byte msgType) {
        //Transport header
        short len = (short) (payload.length + 29);//29 - message header
        byte[] lenBytes = TCPUtil.getBytes(len);
        byte high = (byte) ((len >> 8) & 0xFF);
        byte low = (byte) (len & 0xFF);
        byte xor = (byte) (low ^ high);
        this.th = new byte[]{(byte) 255, lenBytes[3], lenBytes[0], xor};

        //message header
        this.mh = new byte[29];
        byte[] seq = TCPUtil.getBytes(seqNo);
        System.arraycopy(seq, 0, this.mh, 0, 4);
        byte[] usr = TCPUtil.getBytes(16, username);
        System.arraycopy(usr, 0, this.mh, 4, 16);
        byte[] usrNo = TCPUtil.getBytes(4, userNo);
        System.arraycopy(usrNo, 0, this.mh, 20, 4);
        System.arraycopy(time, 0, this.mh, 24, 4);
        this.mh[28] = msgType;

        //Payload
        this.payload = payload;
    }

    public TCPMessage() {
    }


    public byte[] getMessage() {
        byte[] msg = new byte[4 + 29 + payload.length];
        System.arraycopy(th, 0, msg, 0, 4);
        System.arraycopy(mh, 0, msg, 4, 29);
        System.arraycopy(payload, 0, msg, 4 + 29, payload.length);
        TCPUtil.print(msg);
        return msg;
    }

    public void setMessage(byte[] msg) {
        TCPUtil.print(msg);
        this.th = new byte[4];
        this.mh = new byte[29];
        System.arraycopy(msg, 0, this.th, 0, 4);
        short len = TCPUtil.getShort(this.th[1], this.th[2]);
        len = (short) (len - 29);//remove mh - 29
        this.payload = new byte[len];
        System.arraycopy(msg, 4, this.mh, 0, 29);
        System.arraycopy(msg, (4 + 29), this.mh, 0, len);
    }


    @Override
    public String toString() {
        StringBuilder sb1 = new StringBuilder();
        for (byte b : this.th) {
            sb1.append(String.format("%02X ", b));
        }
        String t = sb1.toString();
        StringBuilder sb2 = new StringBuilder();
        for (byte b : this.mh) {
            sb2.append(String.format("%02X ", b));
        }
        String m = sb2.toString();
        return String.format("TH: %s, MH: %s, Payload: %s", t, m, new String(this.payload));
    }
}
