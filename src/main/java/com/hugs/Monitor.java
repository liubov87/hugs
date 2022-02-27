package com.hugs;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadPoolExecutor;

import static com.hugs.Utils.sleep;

@RequiredArgsConstructor
public class Monitor implements Runnable{

    private static final Logger log = LogManager.getLogger(Monitor.class);

    private boolean stop = false;
    private final ThreadPoolExecutor executor;

    @Override
    public void run() {
        while (!stop) {
            log.info("Active: {}; Completed: {}", executor.getActiveCount(), executor.getCompletedTaskCount());
            sleep(5_000);
        }
    }

    public void stop() {
        stop = true;
    }
}
