package tz.co.nezatech.dsetp.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {
    /**
     *
     * @param min incl.
     * @param max incl.
     * @return
     */
    public static int get(int min, int max){
        int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
        return randomNum;
    }
}
