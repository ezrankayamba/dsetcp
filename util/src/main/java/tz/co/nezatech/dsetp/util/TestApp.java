package tz.co.nezatech.dsetp.util;

import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.message.FutureContractSubscription;
import tz.co.nezatech.dsetp.util.message.MarketDataType;
import tz.co.nezatech.dsetp.util.message.StartOfDayDownload;
import tz.co.nezatech.dsetp.util.model.Contract;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public class TestApp {
    private String errorMsg;

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty("jdbcUrl", "jdbc:mysql://localhost:3306/agm?useSSL=false");
        props.setProperty("dataSource.user", "agm");
        props.setProperty("dataSource.password", "agm");
        props.setProperty("dataSource.cachePrepStmts", "true");
        props.setProperty("dataSource.prepStmtCacheSize", "250");
        props.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        ConnectionPool.configure(props);

        //read(bytes);
        //String file = "D:\\TempFiles\\msg99Decompressed.txt";
        String file = "D:\\TempFiles\\msg36Compressed.txt";
        //String file = "D:\\TempFiles\\msg59.txt";
        //String file = "D:\\TempFiles\\msg132.txt";
        byte[] bytes = TCPUtil.readMsg(file);
        byte[] decompressed = StartOfDayDownload.decompress(bytes);
        System.out.println(TCPUtil.text(decompressed));
        MarketDataType dataType = MarketDataType.byType(bytes[33]);
        StartOfDayDownload.read(dataType, decompressed);
        /*int poiter = 32;
        MessageType messageType = MessageType.byType(bytes[poiter++]);
        if (messageType == MessageType.BUSINESS_REJECT) {
            int seqNo = TCPUtil.toInt(TCPUtil.extract(bytes, poiter, 4));
            poiter+=4;
            poiter+=3;// skip msg type ref

            int size=bytes[poiter++];
            String errorMsg= new String(TCPUtil.extract(bytes, poiter, size));
            System.out.println(errorMsg);
        }*/
        //FutureContractSubscription.read(bytes);
        //FutureContractSubscription.readDisplayUpdate(bytes, true);
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        //int add = TCPUtil.holidays();
        //System.out.println(add);
        //TCPUtil.updateHolidays(System.getProperty("user.dir")+ File.separator+"data"+File.separator+"holidays.txt");
        //Contract c=new Contract("Test");
    }
}
