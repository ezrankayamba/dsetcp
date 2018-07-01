package tz.co.nezatech.dsetp.util.config;

import java.util.Properties;

public class Config {
    Properties configFile;

    public Config(String res) {
        configFile = new Properties();
        try {
            configFile.load(this.getClass().getClassLoader().getResourceAsStream(res));
        } catch (Exception eta) {
            eta.printStackTrace();
        }
    }

    public String getProperty(String key) {
        String value = this.configFile.getProperty(key);
        return value;
    }

    public String getProperty(String key, String def) {
        String value = this.configFile.getProperty(key, def);
        return value;
    }
}