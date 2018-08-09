package tz.co.nezatech.dsetp.util.handler.http;

import com.sun.net.httpserver.Headers;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.handler.RequestHandler;
import tz.co.nezatech.dsetp.util.message.MessageType;

public class MsgTypeHandler implements RequestHandler {
    public MsgTypeHandler() {
    }

    @Override
    public boolean willHandle(String method, String path) {
        return path.equalsIgnoreCase("/msgtype");
    }

    @Override
    public String handle(Headers headers, String method, String path, String body) {
        byte[] bytes = TCPUtil.hexToBytes(body);
        MessageType type = TCPUtil.msgType(bytes);
        String resp = type.toString();
        return resp;
    }
}
