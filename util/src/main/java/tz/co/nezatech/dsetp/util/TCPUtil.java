package tz.co.nezatech.dsetp.util;

import com.sun.net.httpserver.Headers;
import org.apache.commons.io.IOUtils;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.message.FutureContractSubscription;
import tz.co.nezatech.dsetp.util.message.MarketDataType;
import tz.co.nezatech.dsetp.util.message.MessageType;
import tz.co.nezatech.dsetp.util.message.StartOfDayDownload;
import tz.co.nezatech.dsetp.util.model.Contract;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

public class TCPUtil {

    public static byte[] readMsg(String file) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(new FileInputStream(new File(file)), writer, "utf-8");
        writer.flush();

        return TCPUtil.hexToBytes(writer.toString());
    }

    public static byte[] extract(byte[] src, int from, int size) {
        byte[] tmp = new byte[size];
        System.arraycopy(src,from, tmp, 0, size);
        return tmp;
    }

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    public static short getShort(byte high, byte low) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).put(high).put(low).getShort(0);
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

    public static byte[] intToBytes(final int i, boolean switchEndian) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return switchEndian ? TCPUtil.switchEndian(bb.array()) : bb.array();
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
        byte hour = (byte) now.getHour();
        byte minute = (byte) now.getMinute();
        byte second = (byte) now.getSecond();
        return new byte[]{hour, minute, second, 0};
    }

    public static byte[] dateNow(int daysOffset) {
        byte[] date = new byte[12];
        LocalDateTime now = LocalDateTime.now();
        now = now.plusDays(daysOffset);
        byte[] year = intToBytes(now.getYear(), true);
        System.arraycopy(year, 0, date, 0, year.length);
        byte[] month = intToBytes(now.getMonthValue(), true);
        System.arraycopy(month, 0, date, 4, month.length);
        byte[] day = intToBytes(now.getDayOfMonth(), true);
        System.arraycopy(day, 0, date, 8, day.length);
        return date;
    }

    public static int[] fromDate(byte[] date) {
        int size = 4;
        int pos = 0;
        byte[] year = new byte[size];
        System.arraycopy(date, pos, year, 0, size);
        pos += size;//4
        year = TCPUtil.switchEndian(year);

        byte[] month = new byte[size];
        System.arraycopy(date, pos, month, 0, size);
        pos += size;//8
        month = TCPUtil.switchEndian(month);

        byte[] day = new byte[size];
        System.arraycopy(date, pos, day, 0, size);
        day = TCPUtil.switchEndian(day);

        return new int[]{TCPUtil.toInt(year), TCPUtil.toInt(month), TCPUtil.toInt(day)};
    }

    public static byte[] bytesEmpty(int num) {
        byte[] tmp = new byte[num];
        for (int i = 0; i < num; i++) tmp[i] = 0;
        return tmp;
    }

    public static byte[] dateEmpty() {
        byte[] date = new byte[12];
        byte[] year = bytesEmpty(4);
        System.arraycopy(year, 0, date, 0, year.length);
        byte[] month = bytesEmpty(4);
        System.arraycopy(month, 0, date, 4, month.length);
        byte[] day = bytesEmpty(4);
        System.arraycopy(day, 0, date, 8, day.length);
        return date;
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

    public static byte[] contract(String name) {
        Contract cont = new Contract(name);
        return cont.toBytes();
    }

    public static byte[] switchEndian(byte[] bytes) {
        byte[] tmp = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            tmp[i] = bytes[bytes.length - i - 1];
        }
        return tmp;
    }

    public static byte[] hexToBytes(String s) {
        s = s.trim().replace(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static MessageType msgType(byte[] bytes) {
        return MessageType.byType(bytes[4 + 29 - 1]);
    }

    public static Map readMessage(Headers headers, MessageType type, String body) {
        Map params = new LinkedHashMap();
        params.put("MessageType", type);
        System.out.println(type);
        switch (type) {
            case START_OF_DAY_DOWNLOAD:
                params.putAll(readStartOfDay(headers, body));
                break;
            case FUTURE_CONTRACT_SUBSCRIPTION:
                params.putAll(readFutureContractSubscr(body));
                break;
        }
        return params;
    }

    public static Map readFutureContractSubscr(String body) {
        byte[] bytes = hexToBytes(body);
        return FutureContractSubscription.read(bytes);
    }

    public static String timeText(byte[] tmp) {
        DecimalFormat df = new DecimalFormat("00");
        return new StringBuilder().append(df.format((int) tmp[0])).append(":").append(df.format((int) tmp[1])).append(":").append(df.format((int) tmp[2])).toString();
    }

    public static Integer toInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static Long toLong(byte[] bytes){
        return ByteBuffer.wrap(bytes).getLong();
    }
    public static Long toLong(byte[] bytes, boolean switchEndian){
        return toLong(switchEndian?TCPUtil.switchEndian(bytes):bytes);
    }

    public static Integer toInt(byte[] bytes, boolean switchEndian) {
        return toInt(switchEndian?TCPUtil.switchEndian(bytes):bytes);
    }

    public static Double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static Double toDouble(byte[] bytes, boolean switchEndian) {
        return toDouble(switchEndian?TCPUtil.switchEndian(bytes):bytes);
    }

    private static Map readStartOfDay(Headers headers, String body) {
        String dt = headers.getFirst("DATA-TYPE");
        MarketDataType dataType = MarketDataType.byType((byte) Integer.parseInt(dt));
        byte[] bytes = hexToBytes(body);
        return StartOfDayDownload.read(dataType, bytes);
    }

    public static List<byte[]> split(byte[] pattern, byte[] input) {
        List<byte[]> l = new LinkedList<>();
        String text = TCPUtil.text(input).trim();
        String p = TCPUtil.text(pattern).trim();
        int inner = text.indexOf(p);
        final int len = input.length;
        boolean firstRead = false;
        for (int i = 0; i < len; ) {
            int indexOf = text.indexOf(p, i + 1);
            if (indexOf > i) {
                int newI = indexOf;
                String sub = null;
                if (inner == 0) {
                    sub = text.substring(i, newI);
                } else if (firstRead) {
                    sub = text.substring(i - inner, newI - inner);
                }
                if (sub != null) {
                    byte[] sec = TCPUtil.hexToBytes(sub);
                    l.add(sec);
                } else {
                    firstRead = true;
                }
                i = newI;
            } else {
                break;
            }
        }
        return l;
    }

    public static List<byte[]> split(int size, byte[] input) {
        List<byte[]> l = new LinkedList<byte[]>();
        String text = TCPUtil.text(input).trim();
        final int len = input.length;
        for (int i = 0; i < len; ) {
            int newI = i + size;
            String sub = text.substring(i, newI);
            byte[] sec = TCPUtil.hexToBytes(sub);
            l.add(sec);
            i = newI;
        }
        return l;
    }
}
