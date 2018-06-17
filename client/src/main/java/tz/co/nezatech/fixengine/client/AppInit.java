package tz.co.nezatech.fixengine.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import quickfix.SocketInitiator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@PropertySource("classpath:client.properties")
public class AppInit implements ApplicationListener<ContextRefreshedEvent> {
    Logger logger = LoggerFactory.getLogger(AppInit.class);
    @Autowired
    SocketInitiator socketInitiator;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.debug(String.format("AppContext: %s, Event: %s", event.getApplicationContext().getApplicationName(), event.getSource()));
        startFixEngine();
    }

    private void startFixEngine() {
        try {
            socketInitiator.start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception: " + e);
            if (socketInitiator != null && socketInitiator.isLoggedOn()) {
                socketInitiator.stop(true);
            }
            logger.info("Client appl terminated");
        }
    }

    @Scheduled(fixedRate = 5000)
    public void serverStatus() {
        logger.info("Client status | Logged On: {}, Current Time: {}", socketInitiator.isLoggedOn(),  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
    }
}
