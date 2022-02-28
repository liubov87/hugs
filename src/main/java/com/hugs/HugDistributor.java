package com.hugs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static com.hugs.Utils.sleep;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class HugDistributor {

    private static final Logger log = LogManager.getLogger(HugDistributor.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<URL, List<Hugger>> URL_HUGGER_MAP = new HashMap<>();
    private static long lastRefresh = System.currentTimeMillis();
    private static boolean stop = false;

    public static void beginHugging(Conf conf) throws IOException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(conf.threads(), conf.threads(), conf.timeLimit(), MINUTES, new LinkedBlockingQueue<>());
        Monitor monitor = new Monitor(executor);
        Thread thread = new Thread(monitor);
        thread.start();

        List<Hugger> huggers = extend(parseHuggerList(conf), conf.threads()).stream().parallel()
                .map(url -> {
                    Hugger hugger = new Hugger(url, conf);
                    synchronized (URL_HUGGER_MAP) {
                        registerHugger(url, hugger);
                    }
                    return hugger;
                })
                .collect(toList());
        huggers.forEach(executor::execute);

        log.info("Extended list: {}", extend(parseHuggerList(conf), conf.threads()));

        //Refresh targets and stop/start redundant/new hugs
        if (conf.receiversRefreshPeriodMinutes() > 0) {
            new Thread(() -> {
                while (!stop) {
                    tryToRefreshHuggersList(conf);
                }
            }).start();
        }

        // Finish if needed
        if (!conf.hugUnresponsiveReceiver() && conf.timeLimit() > 0) {
            // Amount of wait before ending. Either get from conf or allow 5 minute per url
            long schedulingTimeMinutes = conf.timeLimit() > 0 ? conf.timeLimit() : huggers.size() * 5;
            sleep(schedulingTimeMinutes * 60 * 1000);

            end(executor, huggers);
            monitor.stop();
            stop = true;
        }
    }

    private static void tryToRefreshHuggersList(Conf conf) {
        if (System.currentTimeMillis() - lastRefresh < conf.receiversRefreshPeriodMinutes() * 60_000L) {
            sleep(1_000);
            return;
        }

        lastRefresh = System.currentTimeMillis();

        try {
        List<URL> updatedUrlList = extend(parseHuggerList(conf), conf.threads());

        synchronized (URL_HUGGER_MAP) {
            if (new HashSet<>(updatedUrlList).equals(URL_HUGGER_MAP.keySet())) {
                log.info("Target URL list is not changed");
                return;
            }

            List<URL> urlToRemove = new ArrayList<>(URL_HUGGER_MAP.keySet());
            urlToRemove.removeAll(updatedUrlList);
            log.info("Removing redundant huggers: {}", urlToRemove);
            for (URL url : urlToRemove) {
                URL_HUGGER_MAP.getOrDefault(url, Collections.emptyList())
                    .forEach(Hugger::stop);
                URL_HUGGER_MAP.remove(url);
            }

            List<URL> urlToAdd = new ArrayList<>(updatedUrlList);
            urlToAdd.removeAll(URL_HUGGER_MAP.keySet());
            log.info("Adding new huggers: {}", urlToAdd);
            for (URL url : urlToAdd) {
                registerHugger(url, new Hugger(url, conf));
            }
        }
        } catch (Exception e) {
            log.error("Unable to refresh target url list. Keeping old");
        }
    }

    private static void registerHugger(URL url, Hugger hugger) {
        List<Hugger> existingHuggers = URL_HUGGER_MAP.getOrDefault(url,
            new ArrayList<>());
        existingHuggers.add(hugger);
        URL_HUGGER_MAP.put(url, existingHuggers);
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

    /**
     * Extends list of URLs to an amount >= allocated thread count
     * @param urls - list of target urls
     * @param threadCount - how many threads do we want
     * @return
     */
    private static List<URL> extend(List<URL> urls, int threadCount) {
        return Collections.nCopies(threadCount / urls.size() + 1, urls).stream()
                .flatMap(List::stream)
                .limit(max(threadCount, urls.size()))
                .collect(toList());
    }

    private static List<URL> parseHuggerList(Conf conf) throws IOException {
        List<URL> urls;
        String receivers = conf.receivers();
        if (receivers != null && !receivers.trim().isEmpty()) {
            if (receivers.toLowerCase().endsWith(".json")) {
                urls = getUrlsFromJson(receivers);
            } else {
                urls = getUrlsFromPlainText(receivers);
            }
        } else {
            urls = new ArrayList<>();
            urls.add(new URL(conf.receiver()));
        }
        log.info("List of urls: {}", urls);

        return urls;
    }

    private static List<URL> getUrlsFromJson(String receivers) throws IOException {
        try (InputStream inputStream = getInputStream(receivers)) {
            return OBJECT_MAPPER.readValue(inputStream, new TypeReference<List<Site>>() {
                })
                .stream()
                .filter(site -> site.getAtack() > 0)
                .map(Site::getUrl)
                .map(url -> {
                    try {
                        return new URL(url);
                    } catch (MalformedURLException e) {
                        log.error("Invalid URL: " + url, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        }
    }

    private static List<URL> getUrlsFromPlainText(String receivers) throws IOException {
        List<URL> urls = new ArrayList<>();

        InputStreamReader reader = new InputStreamReader(getInputStream(receivers));
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
        return urls;
    }

    private static InputStream getInputStream(String receivers) throws IOException {
        return receivers.startsWith("http") ? new URL(receivers).openStream() : new FileInputStream(receivers);
    }

}
