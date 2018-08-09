package tz.co.nezatech.dsetp.util.handler.http;

import com.sun.net.httpserver.Headers;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.handler.RequestHandler;

public class StreamSectionsHandler implements RequestHandler {
    @Override
    public boolean willHandle(String method, String path) {
        boolean match = path.equalsIgnoreCase("/streamsections");
        return match;
    }

    @Override
    public String handle(Headers headers, String method, String path, String body) {
        System.out.println("Handling stream sectioning");
        StringBuilder sb = new StringBuilder();
        try {
            String key = headers.getFirst("HEX-SEARCH-KEY");
            TCPUtil.split(TCPUtil.hexToBytes(key), TCPUtil.hexToBytes(body)).forEach(bytes -> {
                sb.append(TCPUtil.text(bytes)).append("\r\n");
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Done, handling stream sectioning");
        return sb.toString();
    }
}
