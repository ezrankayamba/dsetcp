package tz.co.nezatech.dsetp.util.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class MyHttpHandler implements HttpHandler {
    Logger logger = LoggerFactory.getLogger(MyHttpHandler.class);
    List<RequestHandler> rhs = new LinkedList<>();

    public MyHttpHandler() {
    }

    public MyHttpHandler(RequestHandler[] handlers) {
        rhs = Arrays.asList(handlers);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        logger.debug("Request Method: " + method);
        Headers headers = exchange.getRequestHeaders();

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        logger.debug("Path: " + path);

        String body = null;
        if (RequestHandler.Method.valueOf(method) == RequestHandler.Method.POST) {
            InputStream requestBody = exchange.getRequestBody();
            StringWriter writer = new StringWriter();
            IOUtils.copy(requestBody, writer, "UTF-8");
            String request = writer.toString();
            logger.debug("HTTP << " + request);
            body = request;
        }
        Optional<RequestHandler> result = rhs.stream().filter(obj -> obj.willHandle(method, path)).findFirst();
        String finalBody = body;
        result.ifPresentOrElse(handler -> {
            String resp = handler.handle(headers, method, path, finalBody);
            logger.debug("HTTP >> " + resp);
            try {
                exchange.sendResponseHeaders(200, resp.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resp.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, () -> {
            logger.debug("No handler found");
            try {
                String resp = "No handler found";
                exchange.sendResponseHeaders(500, resp.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resp.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void addReqHandler(RequestHandler handler) {
        rhs.add(handler);
    }
}
