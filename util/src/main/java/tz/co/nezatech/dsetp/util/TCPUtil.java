package tz.co.nezatech.dsetp.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;

public class TCPUtil {
    public static short getShort(byte high, byte low) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(high);
        bb.put(low);
        return bb.getShort(0);
    }

    /**
     * @param th Transport header
     * @return the extracted message length, excl. transport header itself
     */
    public static short getMsgLength(byte[] th) {
        return shortFromBytes(new byte[]{th[2], th[1]});
    }

    public static int toInt(byte[] bytes, int offset) {
        int ret = 0;
        for (int i = 0; i < 4 && i + offset < bytes.length; i++) {
            ret <<= 8;
            ret |= (int) bytes[i] & 0xFF;
        }
        return ret;
    }

    /**
     * @param mh Message header
     * @return the sequence number
     */
    public static int seqNo(byte[] mh) {
        return toInt(new byte[]{mh[0], mh[1], mh[2], mh[3]}, 0);
    }

    public static String getString(byte[] bytes, int start, int len) {
        byte[] res = new byte[len];
        System.arraycopy(bytes, start, res, 0, len);
        return new String(res);
    }

    public static byte[] intToBytes(final int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return bb.array();
    }

    public static byte[] shortToBytes(final short i) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(i);
        return bb.array();
    }

    public static short shortFromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getShort();
    }

    public static byte[] timeNow() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        byte hour = (byte) now.getHour();
        byte minute = (byte) now.getMinute();
        byte second = (byte) now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        return new byte[]{hour, minute, second, 0};
    }

    public static String getUsername(byte[] bytes) {
        return getString(bytes, 4, 16);
    }

    public static String getUserNo(byte[] bytes) {
        return getString(bytes, 20, 4);
    }

    public static String getTime(byte[] bytes) {
        return String.format("%d:%d:%s", bytes[24], bytes[25], bytes[26]);
    }

    public static byte[] getBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static void print(byte[] bytes) {
        System.out.println();
        for (byte b : bytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    public static String text(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static byte[] getBytes(int allocated, String value) {
        byte[] res = new byte[allocated];
        try {
            byte[] bytes = value.getBytes("UTF-8");
            System.arraycopy(bytes, 0, res, 0, bytes.length);
            res[bytes.length] = '\0';
            return res;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
