package tz.co.nezatech.dsetcp.client.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.message.FutureContractSubscription;
import tz.co.nezatech.dsetp.util.message.MarketDataType;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.StartOfDayDownload;

import java.io.OutputStream;
import java.util.Map;

public class FSMProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FSMProcessor.class);

    /**
     * This will be triggered every time a message is received from the server and parsed successfully
     *
     * @param state The current state of the FMS, something starting with TCP_CONNECTED
     * @param inMsg
     * @return
     */
    public static FSMState process(FSMState state, InMessage inMsg, OutputStream output, ClientFSM.SessionEstablishedListener listener) throws Exception {
        LOGGER.debug(String.format("State: %s, Msg Type: %s", state, inMsg.getType()));
        FSMState newState = state;
        switch (state) {
            case TCP_CONNECTED: {
                newState = FSMLoginManager.tryLogin(inMsg, output);
                if (newState == FSMState.LOGIN_SENT) {
                    LOGGER.debug("Login sent successfully");
                }
            }
            break;
            case LOGIN_SENT: {
                newState = FSMLoginManager.readLoginResponse(inMsg, output);
                if (newState == FSMState.SESSION_ESTABLISHED) {
                    listener.sessionEstablished(output);
                }
            }
            break;
            case SESSION_ESTABLISHED:
            case MARKET_CLOSING:
            case MARKET_CLOSED: {
                if (inMsg.getType() == MessageType.BUSINESS_REJECT) {
                    handleRejectMsg(inMsg);
                } else {
                    handleTradeOrUpdateMsg(inMsg);
                }
            }
            break;
            default: {
                LOGGER.debug("Unhandled state: " + state);
            }
        }
        return newState;
    }

    private static void handleTradeOrUpdateMsg(InMessage inMsg) {
        MessageType type = inMsg.getType();
        byte[] fullMsg = inMsg.getComplete();
        LOGGER.debug("<< " + TCPUtil.text(fullMsg));
        if (type.equals(MessageType.START_OF_DAY_DOWNLOAD)) {
            LOGGER.debug("Uncompress the msg(36) GZIP");
            try {
                MarketDataType dataType = MarketDataType.byType(fullMsg[33]);// 34th byte
                byte[] msg = StartOfDayDownload.decompress(fullMsg);
                LOGGER.debug("Decompressed " + MessageType.START_OF_DAY_DOWNLOAD + " :" + TCPUtil.text(msg));
                StartOfDayDownload.read(dataType, msg);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("Failed to decode the message: " + type);
            }
        } else if (type.equals(MessageType.FUTURE_CONTRACT_SUBSCRIPTION)) {
            LOGGER.debug("Uncompress the msg(36) GZIP");
            try {
                byte[] msg = FutureContractSubscription.decompress(fullMsg);
                LOGGER.debug("Decompressed " + MessageType.FUTURE_CONTRACT_SUBSCRIPTION + " :" + TCPUtil.text(msg));
                Map map = FutureContractSubscription.read(msg);
                LOGGER.debug("Params: " + map);
            } catch (Exception e) {
                LOGGER.error("Failed to decode the message: " + type);
            }
        } else if (type.equals(MessageType.DISPLAY_UPDATES)) {
            byte[] msg = inMsg.getBody();
            try {
                LOGGER.debug("New Update - " + MessageType.DISPLAY_UPDATES + " :" + TCPUtil.text(msg));
                Map map = FutureContractSubscription.readDisplayUpdate(msg, true);
                LOGGER.debug("Params: " + map);
            } catch (Exception e) {
                LOGGER.error("Failed to decode the message: " + type);
            }
        } else {
            byte[] msg = inMsg.getBody();
            LOGGER.debug("<< Application message: " + type + " = " + TCPUtil.text(msg));
        }
    }

    private static void handleRejectMsg(InMessage inMsg) {
        try {
            byte[] fullMsg = inMsg.getComplete();
            LOGGER.debug("<< Reject Msg: " + TCPUtil.text(fullMsg));
            int pointer = 33;
            int seqNo_ = TCPUtil.toInt(TCPUtil.extract(fullMsg, pointer, 4), true);
            pointer += 4;

            int size = fullMsg[pointer++];
            byte msgTypeBytes[] = new byte[]{0, 0, 0, 0};
            byte[] extract = TCPUtil.extract(fullMsg, pointer, size);
            System.out.println("Msg Type Hex: " + TCPUtil.text(extract));
            pointer += size;
            for (int i = 0; i < 4; i++) {
                if (i >= (4 - size)) {
                    msgTypeBytes[i] = extract[i - size];
                }
            }

            System.out.println("Msg Type Hex: " + TCPUtil.text(msgTypeBytes));
            int msgTypeInt = TCPUtil.toInt(msgTypeBytes);
            System.out.println("Msg Type Int: " + msgTypeInt);
            MessageType messageType = MessageType.byType((byte) msgTypeInt);

            size = fullMsg[pointer++];
            String errorMsg = new String(TCPUtil.extract(fullMsg, pointer, size));
            LOGGER.error("Business Reject Msg(" + seqNo_ + ", " + messageType + "): " + errorMsg);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}
