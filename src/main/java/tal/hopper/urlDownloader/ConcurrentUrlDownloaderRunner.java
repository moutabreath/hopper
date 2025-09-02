package tal.hopper.urlDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.config_parser.DownloadConfigParser;
import tal.hopper.urlDownloader.data.URLDownloadResult;
import tal.hopper.urlDownloader.url_downloader.URLDownloader;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
        for (int i = 0; i < downloadConfig.urls.size(); i++) {
            String url = downloadConfig.urls.get(i);
//            futures.add(ecs.submit(new DownloadTask(client, cfg, url, cfg.outputPath(), i + 1)));
            urlDownloader.downloadContent(client, url, i + 1, downloadConfig.maxDownloadTimePerUrl, Path.of(downloadConfig.outputDirectory));
        }


    }
}
