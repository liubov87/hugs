package com.hugs;

import java.util.*;

import com.hugs.service.Hugger;
import com.hugs.service.Monitor;
import com.hugs.service.Shutdown;
import com.hugs.service.UrlRefresh;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class HugDistributor {

    private static final Logger log = LogManager.getLogger(HugDistributor.class);

    private static final Map<URL, List<Hugger>> URL_HUGGER_MAP = new ConcurrentHashMap<>();

    public static void beginHugging(Conf conf) {
        final ThreadPoolExecutor hugPool = new ThreadPoolExecutor(conf.threads(), conf.threads(), conf.timeLimit(), MINUTES, new LinkedBlockingQueue<>());
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        final Monitor monitor = new Monitor(hugPool);
        scheduler.scheduleAtFixedRate(monitor, 5, 5, SECONDS);
        if (conf.receiversRefreshPeriodMinutes() > 0){
            final UrlRefresh refresh = new UrlRefresh(hugPool, conf, URL_HUGGER_MAP);
            scheduler.scheduleAtFixedRate(refresh, 0, conf.receiversRefreshPeriodMinutes(), MINUTES);
        }
        if (conf.timeLimit() > 0){
            final Shutdown shutdown = new Shutdown(hugPool, scheduler, URL_HUGGER_MAP, monitor);
            scheduler.schedule(shutdown, conf.timeLimit(), MINUTES);
        }
    }

}
