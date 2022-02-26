package com.hugs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.hugs.Utils.sleep;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class HugDistributor {

    private static final Logger log = LogManager.getLogger(HugDistributor.class);

    public static void beginHugging(Conf conf) throws IOException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(conf.threads(), conf.threads(), conf.timeLimit(), MINUTES, new LinkedBlockingQueue<>());
        Monitor monitor = new Monitor(executor);
        Thread thread = new Thread(monitor);
        thread.start();

        List<Hugger> huggers = parseHuggerList(conf).stream().parallel()
                .map(url -> new Hugger(url, conf))
                .collect(toList());
        huggers.forEach(executor::execute);

        // Finish if needed
        if (!conf.hugUnresponsiveReceiver() && conf.timeLimit() > 0) {
            // Amount of wait before ending. Either get from conf or allow 5 minute per url
            long schedulingTimeMinutes = conf.timeLimit() > 0 ? conf.timeLimit() : huggers.size() * 5;
            sleep(schedulingTimeMinutes * 60 * 1000);

            end(executor, huggers);
            monitor.stop();
        }
    }

    private static void end(ExecutorService executor, List<Hugger> huggers) {
        executor.shutdown();
        log.info("Waiting for remaining hugs to complete");
        try {
            if (!executor.awaitTermination(30, SECONDS)) {
                huggers.forEach(Hugger::stop); sleep(100);
                executor.shutdownNow(); sleep(100);
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static List<URL> parseHuggerList(Conf conf) throws IOException {
        List<URL> urls = new ArrayList<>();
        if (conf.receivers() != null && !conf.receivers().isBlank()){
            InputStream is = conf.receivers().startsWith("http") ? new URL(conf.receivers()).openStream() : new FileInputStream(conf.receivers());
            InputStreamReader reader = new InputStreamReader(is);
            try(BufferedReader br = new BufferedReader(reader)){
                String s;
                while((s = br.readLine()) != null){
                    try {
                        urls.add(new URL(s));
                    } catch (MalformedURLException e) {
                        log.error("Invalid URL: "+s, e);
                    }
                }
            }
        } else {
            urls.add(new URL(conf.receiver()));
        }
        log.info("List of urls: {}", urls);
        return urls;
    }

}
