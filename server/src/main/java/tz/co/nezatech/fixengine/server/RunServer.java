package tz.co.nezatech.fixengine.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RunServer {

    public static void main(String[] args) {
        SpringApplication.run(RunServer.class, args);
    }
}
