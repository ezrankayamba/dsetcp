package tz.co.nezatech.fixengine.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import quickfix.*;

@Component
public class FixClientApp extends MessageCracker implements Application {
    Logger logger = LoggerFactory.getLogger(FixClientApp.class);

    @Override
    protected void onMessage(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        super.onMessage(message, sessionID);
        logger.debug(String.format("onMessage -> Session ID: %s, Message: %s", sessionID.getSessionQualifier(), message.toString()));
    }

    @Override
    public void onCreate(SessionID sessionID) {
        logger.debug(String.format("onCreate -> Session ID: %s", sessionID.getSessionQualifier()));
    }

    @Override
    public void onLogon(SessionID sessionID) {
        logger.debug(String.format("onLogon -> Session ID: %s", sessionID.getSessionQualifier()));
    }

    @Override
    public void onLogout(SessionID sessionID) {
        logger.debug(String.format("onLogout -> Session ID: %s", sessionID.getSessionQualifier()));
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        logger.debug(String.format("toAdmin -> Session ID: %s, Message: %s", sessionID.getSessionQualifier(), message.toString()));
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.debug(String.format("fromAdmin -> Session ID: %s, Message: %s", sessionID.getSessionQualifier(), message.toString()));
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        logger.debug(String.format("toApp -> Session ID: %s, Message: %s", sessionID.getSessionQualifier(), message.toString()));
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.debug(String.format("fromApp -> Session ID: %s, Message: %s", sessionID.getSessionQualifier(), message.toString()));
    }
}
