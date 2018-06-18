package tz.co.nezatech.fixengine.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import quickfix.ThreadedSocketAcceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@PropertySource("classpath:server.properties")
public class AppInit implements ApplicationListener<ContextRefreshedEvent> {
    Logger logger = LoggerFactory.getLogger(AppInit.class);
    @Autowired
    ThreadedSocketAcceptor socketAcceptor;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.debug(String.format("AppContext: %s, Event: %s", event.getApplicationContext().getApplicationName(), event.getSource()));
        startFixEngine();
    }

    private void startFixEngine() {
        try {
            //socketAcceptor.start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception: " + e);
            if (socketAcceptor != null && socketAcceptor.isLoggedOn()) {
                socketAcceptor.stop(true);
            }
            logger.info("Server appl terminated");
        }
    }

    @Scheduled(fixedRate = 5000)
    public void serverStatus() {
        logger.info("Server status | Logged On: {}, Current Time: {}", socketAcceptor.isLoggedOn(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
    }
}
