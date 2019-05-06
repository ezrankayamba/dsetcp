package tz.co.nezatech.dsetcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

public class MsgSequencer {
    private static Logger logger = LoggerFactory.getLogger(MsgSequencer.class);
    private static Integer seq = -1;

    public static synchronized Integer next() {
        seq++;
        logger.debug("Generated new sequence number: " + seq);
        return seq;
    }

    public static synchronized void reset() {
        seq = -1;
        logger.debug("Sequence reset: " + seq);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 2000; i++) {
            new Thread(new Tester(i)).start();
        }
    }
}

class Tester implements Runnable {
    int id;

    Tester(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        Integer next = MsgSequencer.next();
        LocalTime time = LocalTime.now();
        System.out.println("Tester: " + String.format("%04d = %04d", id, next) + ", " + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond());
    }
}