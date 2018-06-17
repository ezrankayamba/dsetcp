package tz.co.nezatech.fixengine.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;
import tz.co.nezatech.fixengine.server.FixServerApp;

@Configuration
public class FixServerConfig {
    Logger logger = LoggerFactory.getLogger(FixServerConfig.class);
    @Value("${fix.client.cfg.file:server.cfg}")
    private String cfgFile;
    @Autowired
    FixServerApp serverApp;

    @Bean
    public ThreadedSocketAcceptor socketAcceptor() {
        ThreadedSocketAcceptor  acceptor = null;
        try {
            SessionSettings initiatorSettings = new SessionSettings(cfgFile);

            FileStoreFactory msgStoreFactory = new FileStoreFactory(initiatorSettings);
            FileLogFactory fileLogFactory = new FileLogFactory(initiatorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            acceptor = new ThreadedSocketAcceptor (serverApp, msgStoreFactory, initiatorSettings, fileLogFactory, messageFactory);
        } catch (ConfigError | RuntimeError e) {
            e.printStackTrace();
            logger.error("Exception: " + e);
            if (acceptor != null) {
                acceptor.stop(true);
            }
            logger.info("Appl terminated");
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return acceptor;
    }
}
