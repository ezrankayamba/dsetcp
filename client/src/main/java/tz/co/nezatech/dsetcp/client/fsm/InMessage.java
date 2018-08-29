package tz.co.nezatech.dsetcp.client.fsm;

import tz.co.nezatech.dsetp.util.message.MessageType;

public class InMessage {
    private MessageType type;
    private byte [] complete;
    private byte [] body;
    private byte [] msgHeader;
    private byte [] transHeader;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public byte[] getComplete() {
        return complete;
    }

    public void setComplete(byte[] complete) {
        this.complete = complete;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getMsgHeader() {
        return msgHeader;
    }

    public void setMsgHeader(byte[] msgHeader) {
        this.msgHeader = msgHeader;
    }

    public byte[] getTransHeader() {
        return transHeader;
    }

    public void setTransHeader(byte[] transHeader) {
        this.transHeader = transHeader;
    }
}
