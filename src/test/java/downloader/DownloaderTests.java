package downloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.config_parser.DownloadConfigParser;
import tal.hopper.urlDownloader.url_downloader.URLDownloader;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

public class DownloaderTests {

    @Test
    @DisplayName("Test it is possible to download actual file without errors")
    void testActualFileDownloadSuccessfully() {
        // Arrange
        String testConfigFile = "actual-file-config.json";

        // Act
        DownloadConfig downloadConfig = DownloadConfigParser.getDownloadConfiguration(testConfigFile);
        URLDownloader urlDownloader = new URLDownloader();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(downloadConfig.maxDownloadTimePerUrl))
                .version(HttpClient.Version.HTTP_2)
                .build();
        for (int i = 0; i < downloadConfig.urls.size(); i++) {
            String url = downloadConfig.urls.get(i);
            urlDownloader.downloadContent(client, url, downloadConfig.maxDownloadTimePerUrl, Path.of(downloadConfig.outputDirectory));
        }


    }
}
