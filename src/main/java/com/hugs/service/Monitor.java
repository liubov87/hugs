package com.hugs.service;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadPoolExecutor;

@RequiredArgsConstructor
public class Monitor implements Runnable {

    private static final Logger log = LogManager.getLogger(Monitor.class);

    private final ThreadPoolExecutor executor;

    @Override
    public void run() {
        log.info("Active: {}; Completed: {}", executor.getActiveCount(), executor.getCompletedTaskCount());
    }

}
