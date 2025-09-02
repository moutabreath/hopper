package tal.hopper.urlDownloader.url_downloader;

import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.data.URLDownloadResult;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class URLDownloadTask implements Callable<URLDownloadResult> {


    private final HttpClient client;
    private final DownloadConfig config;
    private final String url;
    private final Path outDir;
    private final  URLDownloader urlDownloader;


public URLDownloadTask(HttpClient client, DownloadConfig config, String url, Path outDir, int ordinal, URLDownloader urlDownloader) {
        this.client = client;
        this.config = config;
        this.url = url;
        this.outDir = outDir;
        this.urlDownloader = urlDownloader;
    }


    @Override
    public URLDownloadResult call() {
        return this.urlDownloader.downloadContent(client, this.url, config.maxDownloadTimePerUrl, outDir);
    }
}
