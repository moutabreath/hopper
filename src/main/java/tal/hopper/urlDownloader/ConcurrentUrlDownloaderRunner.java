package tal.hopper.urlDownloader;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.config_parser.DownloadConfigParser;
import tal.hopper.urlDownloader.data.URLDownloadResult;
import tal.hopper.urlDownloader.url_downloader.URLDownloadTask;
import tal.hopper.urlDownloader.url_downloader.URLDownloader;

public class ConcurrentUrlDownloaderRunner {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentUrlDownloaderRunner.class);
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final URLDownloader urlDownloader = new URLDownloader();

    public static void main(String[] args) {
        DownloadConfig downloadConfig;
        try {
            downloadConfig = DownloadConfigParser.getDownloadConfiguration("");
        } catch (IOException e) {
            log.error("Couldn't parse download configuration, got exception", e);
            return;
        }
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

        Instant t0 = Instant.now();
        for (int i = 0; i < downloadConfig.urls.size(); i++) {
            String url = downloadConfig.urls.get(i);
            // Note: The client's connectTimeout is set per-request, not on the builder.
            // This assumes URLDownloadTask is setting it on the HttpRequest.
            // If not, that's a separate required change in URLDownloadTask.
            futures.add(executorCompletionService.submit(new URLDownloadTask(client, downloadConfig, url,
                     Path.of(downloadConfig.outputDirectory), urlDownloader)));
        }
        int resultsSize = analyzeResults(futures, executorCompletionService);

        // Graceful shutdown sequence
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve the interrupted status
        }
        long wall = Duration.between(t0, Instant.now()).toMillis();
        log.info("ALL DONE | wall-clock {} ms ({} urls)", wall, resultsSize);
    }

    private static int analyzeResults(List<Future<URLDownloadResult>> futures,  CompletionService<URLDownloadResult> executorCompletionService){
        List<URLDownloadResult> results = new ArrayList<>();
        for (Future<URLDownloadResult> _ : futures) {
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
                Thread.currentThread().interrupt(); // Preserve the interrupted status
            }
        }
        return results.size();
    }
}
