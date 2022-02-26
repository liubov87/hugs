package com.hugs;

import com.beust.jcommander.JCommander;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Conf conf = new Conf();
        JCommander parser = JCommander.newBuilder().addObject(conf).build();
        parser.usage();
        parser.parse(args);

        log.info("Parsed config: {}", conf);

        try {
            HugDistributor.beginHugging(conf);
        } catch (Exception e) {
            log.error(e);
        }
    }


}
