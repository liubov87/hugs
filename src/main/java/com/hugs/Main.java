package com.hugs;

import com.beust.jcommander.JCommander;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Conf conf = new Conf();
        JCommander parser = JCommander.newBuilder().addObject(conf).build();
        parser.usage();
        parser.parse(args);

        configureLogLevel(conf);

        log.info("Parsed config: {}", conf);

        try {
            HugDistributor.beginHugging(conf);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static void configureLogLevel(Conf conf) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.getLevel(conf.logLevel().toUpperCase()));
        ctx.updateLoggers();
    }


}
