package com.hugs;

import com.beust.jcommander.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class Conf {

    @Parameter(names = { "-r", "-receiver" }, description = "Hug receiver (for one hugger only)")
    private String receiver; // = "https://news.ru/";

    @Parameter(names = { "-rs", "-receivers" }, description = "URL or file path to the list of targets to hug")
    private String receivers = "https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json";
//    private String receivers = "https://raw.githubusercontent.com/liubov87/hugs/main/urls.txt";
//    private String receivers = "urls.txt";

    @Parameter(names = { "-p", "-port" }, description = "Port to knock on")
    private int port = 80;

    @Parameter(names = { "-th", "-threads" }, description = "Number of threads to use. " +
            "It is safe to use many threads (more than CPU cores) in this app, since it is IO bound and threads sleep majoriy of the time. " +
            "These threads are very cheap, besides OS thread management. Waiting for project Loom and virtual threads to improve it even further." +
            "Anyway, if you are running this on modern computer feel free to crank it up a notch")
    private int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    @Parameter(names = { "-s", "-sockets" }, description = "Number of sockets to use per receiver")
    private int connections = 10;

    @Parameter(names = { "-t", "-time" }, description = "How long to perform hugs (in minutes). Leave at 0 to never stop hugging")
    private int timeLimit = 0;

    @Parameter(names = { "-e", "-endless" }, description = "Keep hugs even if receiver doesn't respond, " +
            "which usually indicates a success of passionate hugs. " +
            "Use false to share hugs with more receivers. " +
            "Use true to concentrate on few receivers")
    private boolean hugUnresponsiveReceiver = false;

    @Parameter(names = { "-f", "-refresh" }, description = "Targets list refresh period (in minutes). Leave at 0 to never refresh. Default = 10 minutes")
    private int receiversRefreshPeriodMinutes = 10;

    @Parameter(names = { "-l", "-log" }, description = "log level: trace, debug, info, error")
    private String logLevel = "info";
}
