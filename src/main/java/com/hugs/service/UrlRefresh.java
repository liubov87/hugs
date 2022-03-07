package com.hugs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugs.Conf;
import com.hugs.model.Site;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hugs.Utils.sleep;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class UrlRefresh implements Runnable {

    private static final Logger log = LogManager.getLogger(UrlRefresh.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<URL, List<Hugger>> urlHuggerMap;
    private final ThreadPoolExecutor huggers;
    private final Conf conf;

    public UrlRefresh(ThreadPoolExecutor huggers, Conf conf, Map<URL, List<Hugger>> urlHuggerMap) {
        this.conf = conf;
        this.huggers = huggers;
        this.urlHuggerMap = urlHuggerMap;
    }

    private void tryToRefreshHuggersList() {
        try {
            List<URL> updatedUrlList = extend(parseHuggerList(conf), conf.threads());
            if (new HashSet<>(updatedUrlList).equals(urlHuggerMap.keySet())) {
                log.info("Target URL list has not changed");
                return;
            }

            List<URL> urlToRemove = new ArrayList<>(urlHuggerMap.keySet());
            urlToRemove.removeAll(updatedUrlList);
            log.info("Removing redundant huggers: {}", urlToRemove);
            for (URL url : urlToRemove) {
                urlHuggerMap.getOrDefault(url, Collections.emptyList()).forEach(Hugger::stop);
                urlHuggerMap.remove(url);
            }

            List<URL> urlToAdd = new ArrayList<>(updatedUrlList);
            urlToAdd.removeAll(urlHuggerMap.keySet());
            log.info("Adding new huggers: {}", urlToAdd);
            for (URL url : urlToAdd) {
                Hugger hugger = new Hugger(url, conf);
                registerHugger(url, hugger);

                huggers.execute(hugger);
            }
        } catch (Exception e) {
            log.error("Unable to refresh target url list. Keeping old");
            log.debug("Reason:", e);
        }
    }

    private void registerHugger(URL url, Hugger hugger) {
        List<Hugger> existingHuggers = urlHuggerMap.getOrDefault(url, new ArrayList<>());
        existingHuggers.add(hugger);
        urlHuggerMap.put(url, existingHuggers);
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
        System.out.println(urls);
        return Collections.nCopies(threadCount / max(urls.size(), 1) + 1, urls).stream()
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
            urls = Arrays.asList(new URL(conf.receiver()));
        }
        log.info("List of urls: {}", urls);

        return urls;
    }

    private static List<URL> getUrlsFromJson(String receivers) throws IOException {
        try (InputStream inputStream = getInputStream(receivers)) {
            Site[] sites = OBJECT_MAPPER.readValue(inputStream, Site[].class);
            log.debug("Raw input: {}", Arrays.asList(sites));
            return Stream.of(sites)
                    .filter(Site::isAtack)
                    .map(Site::getUrl)
                    .map(url -> {
                        try {
                            return new URL(url);
                        } catch (MalformedURLException e) {
                            log.error("Invalid URL: {}", url);
                            log.debug("Reason: ", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        }
    }

    private static List<URL> getUrlsFromPlainText(String receivers) throws IOException {
        List<URL> urls = new ArrayList<>();

        try(InputStreamReader reader = new InputStreamReader(getInputStream(receivers));
            BufferedReader br = new BufferedReader(reader)){
            String line;
            while((line = br.readLine()) != null){
                try {
                    urls.add(new URL(line));
                } catch (MalformedURLException e) {
                    log.error("Invalid URL: {}", line);
                    log.debug("Reason: ", e);
                }
            }
        }
        return urls;
    }

    private static InputStream getInputStream(String receivers) throws IOException {
        return receivers.startsWith("http") ? new URL(receivers).openStream() : new FileInputStream(receivers);
    }

    @Override
    public void run() {
        log.info("Refreshing URL list");
        tryToRefreshHuggersList();
    }

}
