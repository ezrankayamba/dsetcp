package tz.co.nezatech.dsetcp.client.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.message.MessageType;

import java.io.OutputStream;

public class FSMLoginManager {
    static final String LOGIN_FAIL_TEXT = "Logon failed, try again";
    private static final Logger LOGGER = LoggerFactory.getLogger(FSMLoginManager.class);

    public static FSMState tryLogin(InMessage inMsg, OutputStream output) throws Exception {
        if (inMsg.getType() == MessageType.DAILY_KEY) {
            LOGGER.debug("Daily Key Received");
            sendLogin(inMsg, output);
            return FSMState.LOGIN_SENT;
        }
        return FSMState.INIT_TCP;
    }

    private static void sendLogin(InMessage message, OutputStream output) throws Exception {
        FSMTCPWriter.sendLogin(message, output);
    }

    public static FSMState readLoginResponse(InMessage inMsg, OutputStream output) throws Exception {
        LOGGER.debug("<< Login response: " + new String(inMsg.getBody()));
        String msgText = TCPUtil.text(inMsg.getComplete());
        String invalidPwdText = TCPUtil.text("Invalid Password".getBytes());
        MessageType type = inMsg.getType();
        if (type == MessageType.ACK_SUCCESS && !msgText.contains(invalidPwdText)) {
            LOGGER.debug("<< Login success");
            return FSMState.SESSION_ESTABLISHED;
        }

        throw new Exception(LOGIN_FAIL_TEXT);
    }
}
