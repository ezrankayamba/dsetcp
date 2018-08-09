package tz.co.nezatech.dsetp.util.handler.http;

import com.sun.net.httpserver.Headers;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.handler.RequestHandler;

public class SwitchEndianHandler implements RequestHandler {
    @Override
    public boolean willHandle(String method, String path) {
        return path.equalsIgnoreCase("/switchendian");
    }

    @Override
    public String handle(Headers headers, String method, String path, String body) {
        return TCPUtil.text(TCPUtil.switchEndian(TCPUtil.hexToBytes(body)));
    }
}
