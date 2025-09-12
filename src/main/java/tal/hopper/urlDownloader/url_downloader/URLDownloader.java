package tal.hopper.urlDownloader.url_downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tal.hopper.urlDownloader.data.URLDownloadResult;
import tal.hopper.urlDownloader.utils.FileUtils;

public class URLDownloader {

    private static final Logger log = LoggerFactory.getLogger(URLDownloader.class);

    /**
     * Downloads the content from a given URL and returns it as a byte array.
     * This method handles the networking and reads the entire content into memory.
     *
     * @param fileUrl The URL of the file to download.
     * @return A byte array containing the file's content, or {@code null} if an error occurs.
     */
    public URLDownloadResult downloadContent(HttpClient client, String fileUrl, int perUrlTimeoutSeconds, Path outDir) {
        log.info("Starting download for URL: {}", fileUrl);
        final Instant started = Instant.now();
        int statusCode = 0;
        long bytesWritten = 0L;
        String finalName = "";
 
        try {
            final String desiredName = FileUtils.deriveFileName(fileUrl);
            finalName = FileUtils.resolveFileName(outDir, desiredName);
            final URI uri = URI.create(fileUrl);
 
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(perUrlTimeoutSeconds))
                    .GET()
                    .build();
 
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            statusCode = resp.statusCode();
 
            if (statusCode >= 200 && statusCode < 300) {
                Path tmp = outDir.resolve(finalName + ".part");
                try (InputStream is = resp.body()) {
                    bytesWritten = Files.copy(is, tmp);
                }
                Files.move(tmp, outDir.resolve(finalName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Instant end = Instant.now();
                long duration = Duration.between(started, end).toMillis();
                return new URLDownloadResult(fileUrl, finalName, true, statusCode, bytesWritten, duration, null, started, end);
            } else {
                // Try to read the error body for better diagnostics
                String errorBody = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                String errorMessage = String.format("HTTP status %d: %s", statusCode, errorBody.lines().findFirst().orElse(""));
                throw new IOException(errorMessage);
            }
        } catch (IllegalArgumentException | MalformedURLException e) {
            log.error("Invalid URL provided: {}", fileUrl, e);
            long duration = Duration.between(started, Instant.now()).toMillis();
            return new URLDownloadResult(fileUrl, "", false, 0, 0, duration, e.getMessage(), started, Instant.now());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to download from {}: {}", fileUrl, e.getMessage());
            long duration = Duration.between(started, Instant.now()).toMillis();
            return new URLDownloadResult(fileUrl, finalName, false, statusCode, bytesWritten, duration, e.getMessage(), started, Instant.now());
        }
    }
}