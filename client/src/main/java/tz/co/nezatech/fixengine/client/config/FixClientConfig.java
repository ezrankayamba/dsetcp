package tz.co.nezatech.fixengine.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;
import tz.co.nezatech.fixengine.client.FixClientApp;

@Configuration
public class FixClientConfig {
    Logger logger = LoggerFactory.getLogger(FixClientConfig.class);
    @Value("${fix.client.cfg.file:client.cfg}")
    private String cfgFile;

    @Bean
    public SocketInitiator socketInitiator() {
        SocketInitiator initiator = null;
        try {
            SessionSettings initiatorSettings = new SessionSettings(cfgFile);
            Application dseClient = new FixClientApp();

            FileStoreFactory msgStoreFactory = new FileStoreFactory(initiatorSettings);
            FileLogFactory fileLogFactory = new FileLogFactory(initiatorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            initiator = new SocketInitiator(dseClient, msgStoreFactory, initiatorSettings, fileLogFactory, messageFactory);
        } catch (ConfigError | RuntimeError e) {
            e.printStackTrace();
            logger.error("Exception: " + e);
            if (initiator != null) {
                initiator.stop(true);
            }
            logger.info("Appl terminated");
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        logger.debug("Initiator created successfully");
        return initiator;
    }
}
