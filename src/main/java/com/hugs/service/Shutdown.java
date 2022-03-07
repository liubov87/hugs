package com.hugs.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static com.hugs.Utils.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Shutdown implements Runnable {

    private static final Logger log = LogManager.getLogger(Shutdown.class);

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final Map<URL, List<Hugger>> urlHuggerMap;
    private final Monitor monitor;


    public Shutdown(ExecutorService executor, ScheduledExecutorService scheduler, Map<URL, List<Hugger>> urlHuggerMap, Monitor monitor) {
        this.executor = executor;
        this.scheduler = scheduler;
        this.urlHuggerMap = urlHuggerMap;
        this.monitor = monitor;
    }

    private void endHuggers() {
        executor.shutdown();
        log.info("Waiting for remaining hugs to complete");
        try {
            if (!executor.awaitTermination(30, SECONDS)) {
                urlHuggerMap.values().stream().flatMap(List::stream).forEach(Hugger::stop); sleep(100);
                executor.shutdownNow(); sleep(100);
            }
        } catch (InterruptedException e) {
            log.error("Couldn't stop huggers");
            log.debug("Reason", e);
        }
    }

    private void endScheduler() {
        scheduler.shutdown();
        log.info("Shutting down scheduler");
        try {
            if (!scheduler.awaitTermination(1, SECONDS)) {
                scheduler.shutdownNow(); sleep(100);
            }
        } catch (InterruptedException e) {
            log.error("Couldn't stop scheduler");
            log.debug("Reason", e);
        }
    }

    @Override
    public void run() {
        endHuggers();
        endScheduler();
    }
}
