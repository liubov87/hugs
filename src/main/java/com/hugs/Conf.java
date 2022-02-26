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
    private String receiver;// = "https://news.ru/";

    @Parameter(names = { "-rs", "-receivers" }, description = "URL or file path to the list of targets to hug")
    private String receivers = "https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt";
//    private String receivers = "urls.txt";

    @Parameter(names = { "-p", "-port" }, description = "Port to knock on")
    private int port = 80;

    @Parameter(names = { "-th", "-threads" }, description = "Number of threads to use")
    private int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2) * 10;

    @Parameter(names = { "-s", "-sockets" }, description = "Number of sockets to use per receiver")
    private int connections = 10;

    @Parameter(names = { "-t", "-time" }, description = "How long to perform hugs (in minutes). Leave at 0 to never stop hugging")
    private int timeLimit = 0;

    @Parameter(names = { "-e", "-endless" }, description = "Keep hugs even if receiver doesn't respond, " +
            "which usually indicates a success of passionate hugs. " +
            "Use false to share hugs with more receivers. " +
            "Use true to concentrate on few receivers")
    private boolean hugUnresponsiveReceiver = false;
}
