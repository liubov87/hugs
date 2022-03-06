package com.hugs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class Utils {

    private static final Logger log = LogManager.getLogger(Utils.class);

    public static final Random RND = new Random();

    public static void sleep(long minDuration, long maxDuration) {
        sleep(RND.nextInt((int)(maxDuration - minDuration)) + minDuration);
    }

    public static void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    public static String generateField(){
        final String[] prefix = {"user", "page", "key", "item", "index", "idx", "entry", "job", "find", "search", "is"};
        final String[] alphabet = "abcdefghijklmnopqrstuvwxyz".split("");
        final String[] delimiter = {"-", "_", ""};
        final String[] suffix = {"id", "of", "time", "c", "idx", "t", "head", "entry"};

        return oneOf(prefix) + oneOf(alphabet) + oneOf(delimiter) + oneOf(suffix);
    }

    public static String oneOf(String[] arr){
        return arr[RND.nextInt(arr.length)];
    }

}
