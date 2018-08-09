package tz.co.nezatech.dsetp.util.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

public class ConnectionPool {
    private static HikariDataSource ds = null;
    private static Properties dbProps = null;

    public static void configure(Properties dbProps) {
        ConnectionPool.dbProps = dbProps;
    }

    public static HikariDataSource dataSource() throws RuntimeException {
        if(dbProps == null){
            throw new RuntimeException("You must configure this connection poll by calling; configure(Properties dbProps)!");
        }
        if (ds == null) {
            HikariConfig cfg = new HikariConfig(dbProps);
            ds = new HikariDataSource(cfg);
        }
        return ds;
    }
}