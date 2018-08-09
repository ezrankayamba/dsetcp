package tz.co.nezatech.dsetp.util.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.TCPUtil;

public class Contract {
    /**
     * 4 Bytes, E- Equity
     */
    byte[] prefix = new byte[]{'E', 0, 0, 0};
    byte[] date;
    byte[] instrumentName;
    byte callPut = 0;
    byte[] strike = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    byte[] secondaryDate;
    byte[] secondaryInstrumentName = new byte[]{0, 0, 0, 0, 0};
    byte isDeltaOption = 0;

    Logger logger = LoggerFactory.getLogger(Contract.class);

    public Contract(String name) {
        String expireDays = System.getProperty("ExpireDays");
        if (expireDays != null) {
            System.out.println("Use custom " + expireDays + " 3 days");
            date = TCPUtil.dateNow(Integer.parseInt(expireDays));
        } else {
            System.out.println("Use default 3 days");
            date = TCPUtil.dateNow(3);
        }
        secondaryDate = TCPUtil.dateEmpty();
        instrumentName = new byte[5];
        instrumentName[0] = (byte) name.length();
        System.arraycopy(name.toUpperCase().getBytes(), 0, instrumentName, 1, 4);
    }

    public Contract(byte[] bytes) {
        int pos = 0;
        int size = 4;
        System.arraycopy(bytes, pos, prefix, 0, size);// 4 bytes
        pos += size;
        //System.out.println("Pos: "+pos);

        size = 12;
        date = new byte[12];
        System.arraycopy(bytes, pos, date, 0, size);// 4 + 12 = 16 bytes
        pos += size;
        //System.out.println("Pos: "+pos);

        pos++;//skip len bye
        //System.out.println("Pos: "+pos);

        size = 5;
        instrumentName = new byte[size];
        System.arraycopy(bytes, pos, instrumentName, 0, size - 1); // 16 + 5 = 21 bytes
        pos += size - 1;
        //System.out.println("Pos: "+pos);

        size = 8;
        System.arraycopy(bytes, pos, strike, 0, size); // 21 + 8 = 29 bytes
        pos += size;
        //System.out.println("Pos: "+pos);

        callPut = bytes[pos++]; // 29 + 1 = 30 bytes
        //System.out.println("Pos: "+pos);

        size = 12;
        secondaryDate = new byte[12];
        System.arraycopy(bytes, pos, secondaryDate, 0, size); // 30 + 12 = 42 bytes
        pos += size;
        //System.out.println("Pos: "+pos);

        pos++;//skip len bye
        //System.out.println("Pos: "+pos);

        size = 5;
        secondaryInstrumentName = new byte[size];
        System.arraycopy(bytes, pos, secondaryInstrumentName, 0, size - 1); // 42 + 5 = 47 bytes
        pos += size - 1;
        //System.out.println("Pos: "+pos);

        isDeltaOption = bytes[pos++]; // 47 +1 = 48 bytes
        //System.out.println("Pos: "+pos);
    }

    public String getPrimaryName() {
        return new String(instrumentName).trim();
    }
    public String getDate() {
        int[] dateArr = TCPUtil.fromDate(date);
        return String.format("%04d-%02d-%02d", dateArr[0], dateArr[1], dateArr[2]);
    }
    public String getSecDate() {
        int[] dateArr = TCPUtil.fromDate(secondaryDate);
        return String.format("%04d-%02d-%02d", dateArr[0], dateArr[1], dateArr[2]);
    }

    public byte[] toBytes() {
        byte[] tmp = new byte[48];
        int pos = 0;
        System.arraycopy(prefix, 0, tmp, pos, prefix.length);// 4 bytes
        pos += prefix.length;
        System.out.println("Exp Date: " + TCPUtil.text(date));
        System.arraycopy(date, 0, tmp, pos, date.length);// 4 + 12 = 16 bytes
        pos += date.length;
        System.arraycopy(instrumentName, 0, tmp, pos, instrumentName.length); // 16 + 5 = 21 bytes
        pos += instrumentName.length;
        System.arraycopy(strike, 0, tmp, pos, strike.length); // 21 + 8 = 29 bytes
        pos += strike.length;
        tmp[pos++] = callPut; // 29 + 1 = 30 bytes
        System.arraycopy(secondaryDate, 0, tmp, pos, secondaryDate.length); // 30 + 12 = 42 bytes
        pos += secondaryDate.length;
        System.arraycopy(secondaryInstrumentName, 0, tmp, pos, secondaryInstrumentName.length); // 42 + 5 = 47 bytes
        pos += secondaryInstrumentName.length;
        tmp[pos] = isDeltaOption; // 47 +1 = 48 bytes
        return tmp;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%c", (char) prefix[0]));
        sb.append("|").append(getDate());
        sb.append("|").append(new String(instrumentName).trim());
        sb.append("|").append(String.format("%f", TCPUtil.toDouble(strike)));
        sb.append("|").append(String.format("%c", (char) callPut));

        sb.append("|").append(getSecDate());
        sb.append("|").append(new String(secondaryInstrumentName).trim());

        sb.append("|").append((int) isDeltaOption);

        return sb.toString();
    }
}
