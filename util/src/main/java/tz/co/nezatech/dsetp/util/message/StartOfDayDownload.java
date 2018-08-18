package tz.co.nezatech.dsetp.util.message;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.GZipUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class StartOfDayDownload {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartOfDayDownload.class);

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
        } else if (dataType == MarketDataType.INSTRUMENTS_DATA) {
            LOGGER.debug("Holiday Data: " + TCPUtil.text(bytesList));
            int pointer = 0;
            do {

                HikariDataSource ds = ConnectionPool.dataSource();
                try (Connection con = ds.getConnection()){
                    PreparedStatement s = con.prepareStatement("INSERT INTO agm_holiday_data (hd_holiday_sequence, hd_country_code, hd_holiday_date, hd_reminder_only,  hd_is_early_close, hd_is_furures_close_out) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    int size = 4;
                    int seqNo = TCPUtil.toInt(TCPUtil.extract(bytesList, pointer, size), true);
                    pointer += size;
                    s.setObject(1, seqNo);

                    size = TCPUtil.toInt(TCPUtil.extract(bytesList, pointer++, 1), true);
                    String countryCode = new String(TCPUtil.extract(bytesList, pointer, size));
                    pointer += size;
                    s.setObject(2, countryCode);

                    size = 12;
                    byte[] dbts = TCPUtil.extract(bytesList, pointer, size);
                    pointer += size;
                    String date = String.format("%04d-%02d-%02d",
                            TCPUtil.toInt(TCPUtil.extract(dbts, 0, 4), true),
                            TCPUtil.toInt(TCPUtil.extract(dbts, 4, 4), true),
                            TCPUtil.toInt(TCPUtil.extract(dbts, 8, 4), true)
                    );
                    s.setObject(3, date);

                    byte reminderOnly = bytesList[pointer++];
                    s.setObject(4, reminderOnly);

                    byte isEarlyClose = bytesList[pointer++];
                    s.setObject(5, isEarlyClose);

                    byte isFuturesCloseOut = bytesList[pointer++];
                    s.setObject(6, isFuturesCloseOut);
                } catch (SQLException e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage());
                }

            } while (pointer < bytesList.length);

        }
        return imap;
    }

}
