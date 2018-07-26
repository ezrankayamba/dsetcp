package tz.co.nezatech.dsetp.util.model;

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
    byte[] secondaryDate = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    byte[] secondaryInstrumentName = new byte[]{0,0,0,0,0};
    byte isDeltaOption = 0;

    public Contract(String name) {
        date = TCPUtil.dateNow(3);
        instrumentName = new byte[5];
        instrumentName[0] = (byte) name.length();
        System.arraycopy(name.toUpperCase().getBytes(), 0, instrumentName, 1, 4);
    }

    public byte[] toBytes() {
        byte[] tmp = new byte[48];
        int pos = 0;
        System.arraycopy(prefix, 0, tmp, pos, prefix.length);
        pos += prefix.length;
        System.arraycopy(date, 0, tmp, pos, date.length);
        pos += date.length;
        System.arraycopy(instrumentName, 0, tmp, pos, instrumentName.length);
        pos += instrumentName.length;
        System.arraycopy(strike, 0, tmp, pos, strike.length);
        pos += strike.length;
        tmp[pos++] = callPut;
        System.arraycopy(secondaryDate, 0, tmp, pos, secondaryDate.length);
        pos += secondaryDate.length;
        System.arraycopy(secondaryInstrumentName, 0, tmp, pos, secondaryInstrumentName.length);
        pos += secondaryInstrumentName.length;
        tmp[pos] = isDeltaOption;
        return tmp;
    }
}
