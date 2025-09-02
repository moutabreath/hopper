package tal.hopper.urlDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.config_parser.DownloadConfigParser;
import tal.hopper.urlDownloader.data.URLDownloadResult;
import tal.hopper.urlDownloader.url_downloader.URLDownloadTask;
import tal.hopper.urlDownloader.url_downloader.URLDownloader;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ConcurrentUrlDownloaderRunner {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentUrlDownloaderRunner.class);
    public static void main(String[] args) {
        DownloadConfig downloadConfig = DownloadConfigParser.getDownloadConfiguration("");
        if (downloadConfig == null) {
            log.error("Could not parse download configuration. Exiting.");
            return;
        }
        try {
            Files.createDirectories(Path.of(downloadConfig.outputDirectory));
        } catch (IOException e) {
            log.error("Failed to create root directory", e);
            return;
        }

        downloadConcurrently(downloadConfig);
    }

    private static void downloadConcurrently(DownloadConfig downloadConfig) {
        ExecutorService pool = Executors.newFixedThreadPool(downloadConfig.maxConcurrentDownloads);
        CompletionService<URLDownloadResult> executorCompletionService = new ExecutorCompletionService<>(pool);
        List<Future<URLDownloadResult>> futures = new ArrayList<>();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(downloadConfig.maxDownloadTimePerUrl))
                .version(HttpClient.Version.HTTP_2)
                .build();

        URLDownloader urlDownloader = new URLDownloader();
        Instant t0 = Instant.now();
        for (int i = 0; i < downloadConfig.urls.size(); i++) {
            String url = downloadConfig.urls.get(i);
            futures.add(executorCompletionService.submit(new URLDownloadTask(client, downloadConfig, url, Path.of(downloadConfig.outputDirectory), i + 1, urlDownloader)));
        }
        int resultsSize = analyzeResults(futures, executorCompletionService);
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long wall = Duration.between(t0, Instant.now()).toMillis();
        log.info("ALL DONE | wall-clock {} ms ({} urls)", wall, resultsSize);
    }

    private static int analyzeResults(List<Future<URLDownloadResult>> futures,  CompletionService<URLDownloadResult> executorCompletionService){
        List<URLDownloadResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                URLDownloadResult urlDownloadResult = executorCompletionService.take().get(); // completion order
                results.add(urlDownloadResult);
                if (urlDownloadResult.success) {
                    log.info("DONE {} -> {} | {} ms | {} bytes | status {}", urlDownloadResult.uri, urlDownloadResult.fileName, urlDownloadResult.durationMillis, urlDownloadResult.bytesWritten, urlDownloadResult.statusCode);
                } else {
                    log.error("FAIL {} -> {} | {} ms | error: {}", urlDownloadResult.uri, urlDownloadResult.fileName, urlDownloadResult.durationMillis, urlDownloadResult.errorMessage);
                }
            } catch (ExecutionException e) {
                log.error("Unexpected task error: {}", e.getCause().toString());
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for result", e);
            }
        }
        return results.size();
    }
}
