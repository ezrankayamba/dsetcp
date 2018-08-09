package tz.co.nezatech.dsetp.util.handler;

import com.sun.net.httpserver.Headers;

public interface RequestHandler {
    public boolean willHandle(String method, String path);
    public String handle(Headers headers, String method, String path, String body);
    public enum Method{
        POST, GET
    }
}
