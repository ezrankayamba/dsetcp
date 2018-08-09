package tz.co.nezatech.dsetp.util.message;

import com.zaxxer.hikari.HikariDataSource;
import tz.co.nezatech.dsetp.util.GZipUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

public class StartOfDayDownload {
    public static byte[] decompress(byte[] bytes) {
        int headers = 61;//Remove TH(4), MH(29), DH(24) + 4 trailing
        int size = bytes.length - headers;
        byte[] gzip = new byte[size];
        System.arraycopy(bytes, headers, gzip, 0, size);
        try {
            return GZipUtil.decompress(gzip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map read(MarketDataType dataType, byte[] bytesList) {
        int structSize = 478 * 3;// 3 is per hex space formulae
        Map imap = new LinkedHashMap();
        System.out.println("Data Type: " + dataType);
        if (dataType == MarketDataType.INSTRUMENTS_DATA) {
            byte[] pattern = new byte[4];
            System.arraycopy(bytesList, 0, pattern, 0, 4);
            TCPUtil.split(structSize, bytesList).forEach(bytes -> {
                Map params = new LinkedHashMap();
                HikariDataSource ds = ConnectionPool.dataSource();
                try (Connection con = ds.getConnection()) {
                    PreparedStatement s = con.prepareStatement("INSERT INTO agm_instrument (instru_sequence, instru_name, instru_type_number, instru_isin_code,  instru_description) VALUES (?, ?, ?, ?, ?, ?)");

                    int pointer = 0;
                    int p = 0;
                    byte[] tmp = new byte[4];
                    System.arraycopy(bytes, pointer, tmp, 0, 4);
                    pointer += 4;
                    params.put("instru_sequence", TCPUtil.toInt(TCPUtil.switchEndian(tmp), 0));
                    s.setObject(++p, TCPUtil.toInt(TCPUtil.switchEndian(tmp)));

                    pointer += 26;//skip those

                    int size = (int) bytes[pointer++];
                    tmp = new byte[size];
                    System.arraycopy(bytes, pointer, tmp, 0, size);
                    pointer += size;
                    params.put("instru_name", new String(tmp));
                    s.setObject(++p, new String(tmp));

                    String itn = TCPUtil.text(new byte[]{bytes[pointer++]});
                    params.put("instru_type_number", itn);
                    s.setObject(++p, itn);


                    size = (int) bytes[pointer++];
                    tmp = new byte[size];
                    System.arraycopy(bytes, pointer, tmp, 0, size);
                    pointer += size;
                    params.put("instru_isin_code", new String(tmp));
                    s.setObject(++p, new String(tmp));

                    size = (int) bytes[pointer++];
                    tmp = new byte[size];
                    System.arraycopy(bytes, pointer, tmp, 0, size);
                    pointer += size;
                    params.put("instru_description", new String(tmp));
                    s.setObject(++p, new String(tmp));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(params);
            });
        }
        return imap;
    }

}
