package tz.co.nezatech.dsetp.util.config;

public class ConnectionConfig {
    public static Config get(){
        return new Config("connection.cfg");
    }
    public static Config get(String res){
        return new Config(res);
    }
}
