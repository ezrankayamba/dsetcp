package tz.co.nezatech.dsetp.util.handler.http;

import com.sun.net.httpserver.Headers;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.handler.RequestHandler;
import tz.co.nezatech.dsetp.util.message.MessageType;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgReadHandler implements RequestHandler {
    public MsgReadHandler() {
    }

    @Override
    public boolean willHandle(String method, String path) {
        String patternString = "^/readmessage/(\\d{1,3})$";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(path);
        return matcher.matches();
    }

    @Override
    public String handle(Headers headers, String method, String path, String body) {
        String[] split = path.split("/");
        System.out.println(Arrays.asList(split));
        int i = Integer.parseInt(split[2]);
        MessageType type = MessageType.byType((byte) i);
        String resp = TCPUtil.readMessage(headers, type, body).toString();
        return resp;
    }
}
