package tz.co.nezatech.dsetp.util.message;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.GZipUtil;
import tz.co.nezatech.dsetp.util.TCPUtil;
import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.model.Contract;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class FutureContractSubscription {
    static Logger logger = LoggerFactory.getLogger(FutureContractSubscription.class);

    public static byte[] decompress(byte[] bytes) {
        int headers = 38;//Remove TH(4), MH(29), DH(1) + 4 trailing
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

    public static Map read(byte[] bytesList) {
        Map params = new LinkedHashMap();
        byte[] pattern = TCPUtil.extract(bytesList, 0, 4);
        TCPUtil.split(pattern, bytesList).forEach(bytes -> {
            FutureContractSubscription.readDisplayUpdate(bytes);
            //System.out.println(m);
        });
        return params;
    }

    public static Map readDisplayUpdate(byte[] bytes, boolean isUpdate) {
        return readDisplayUpdate(isUpdate ? TCPUtil.extract(bytes, 1, bytes.length - 1) : bytes);
    }

    public static Map readDisplayUpdate(byte[] bytes) {
        Map m = new LinkedHashMap();
        try {
            HikariDataSource ds = ConnectionPool.dataSource();
            try (Connection con = ds.getConnection()) {
                PreparedStatement s = con.prepareStatement("INSERT INTO agm_trading_data (td_contract, td_contract_name, td_mid_price, td_last_dealt_price,  td_last_dealt_time,  td_deal_volume,  td_high_price,  td_low_price,  td_day_volume,  td_last_qty,  td_last_action, td_price, td_change,  td_contract_status,  td_last_traded_qty,  td_update_time,  td_vwap,  td_closing_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                int pointer = 0;

                pointer++;//skip 1, anonymous trading [1]
                int p = 0;// prepared statement index(1 based)

                //contract(48) [49]
                int size = 48;
                byte[] tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                Contract contract = new Contract(tmp);
                m.put("td_contract" + p, contract.toText());
                s.setString(++p, contract.toText());//1
                s.setString(++p, contract.getPrimaryName());//2

                // Mid price (8) [57]
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                double myDouble = TCPUtil.toDouble(tmp, true);
                pointer += size;
                m.put("td_mid_price" + p, myDouble);
                s.setObject(++p, myDouble);//3

                // Last dealt price (8) [65]
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_last_dealt_price" + p, myDouble);
                s.setDouble(++p, myDouble);//4

                // Last dealt time (4) [69]
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_last_dealt_time" + p, contract.getDate() + " " + TCPUtil.timeText(tmp));
                s.setString(++p, contract.getDate() + " " +TCPUtil.timeText(tmp));//5


                // Deal volume (8) [77]
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_deal_volume" + p, TCPUtil.toLong(tmp, true));
                s.setObject(++p, TCPUtil.toLong(tmp, true));//6

                // High price (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_high_price" + p, myDouble);
                s.setDouble(++p, myDouble);//7


                // Low price (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_low_price" + p, TCPUtil.toDouble(tmp));
                s.setDouble(++p, myDouble);


                // Day's Volume
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                Long myLong = TCPUtil.toLong(tmp, true);
                m.put("td_day_volume" + p, myLong);
                s.setObject(++p, myLong);


                //Last order qty(4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_last_qty" + p, TCPUtil.toInt(tmp, true));
                s.setObject(++p, TCPUtil.toInt(tmp, true));

                //Last order buy sell(1)/action
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                OrderAction action = OrderAction.byType(tmp[0]);
                m.put("td_last_action" + p, action);
                s.setString(++p, action.toString());


                // Last order price (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_price" + p, myDouble);
                s.setDouble(++p, myDouble);


                //Number of depth (1)
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                int numberOfDepth = (int) tmp[0];
                m.put("NumberOfDepth", numberOfDepth);


                //Open interest (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                // Change(8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_change" + p, myDouble);
                s.setDouble(++p, myDouble);


                //Auction (1)
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));

                //Contract status (1)
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_contract_status" + p, (int) tmp[0]);
                s.setObject(++p, (int) tmp[0]);

                //Odd Lot (1)
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                // Last traded qty (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_last_traded_qty" + p, TCPUtil.toLong(tmp, true));
                s.setObject(++p, TCPUtil.toLong(tmp, true));

                //Date Sequence (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Sec. Date Sequence (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Strike Sequence (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Market Shard.. Sequence (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Stack Sequence (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Unused (21)
                size = 21;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));


                //Update time (4)
                size = 4;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                m.put("td_update_time" + p, contract.getDate() + " " + TCPUtil.timeText(tmp));
                s.setString(++p, contract.getDate() + " " +TCPUtil.timeText(tmp));

                //VWAP (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_vwap" + p, myDouble);
                s.setDouble(++p, myDouble);


                //Closing price (8)
                size = 8;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                myDouble = TCPUtil.toDouble(tmp, true);
                m.put("td_closing_price" + p, myDouble);
                s.setDouble(++p, myDouble);


                //Reversed (1)
                size = 1;
                tmp = new byte[size];
                System.arraycopy(bytes, pointer, tmp, 0, size);
                pointer += size;
                //params.put("td_last_action", TCPUtil.text(tmp));

                int records = s.executeUpdate();
                System.out.println("Recorded new entry for trade data: " + records);

                System.out.println(m);

            } catch (SQLException e) {
                System.err.println(e.getErrorCode());
                logger.error(e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        return m;
    }
}
