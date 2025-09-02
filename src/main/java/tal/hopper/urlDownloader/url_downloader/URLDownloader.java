package tal.hopper.urlDownloader.url_downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tal.hopper.urlDownloader.data.URLDownloadResult;
import tal.hopper.urlDownloader.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

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
        Instant started = Instant.now();
        int status = 0;
        long written = 0L;

        String desiredName = FileUtils.deriveFileName(fileUrl);
        String finalName;
        HttpRequest req;
        try {
            URI uri = URI.create(fileUrl);
            log.debug("URL object created successfully for: {}", fileUrl);
            finalName = FileUtils.resolveFileName(outDir, desiredName);
            req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(perUrlTimeoutSeconds))
                    .GET()
                    .build();
        }
        catch(Exception ex){
            log.error("Invalid URL provided: {}", fileUrl, ex);
            return new URLDownloadResult(fileUrl, "", false, status, written, 0, ex.getMessage(), started, Instant.now());
        }
        if (req == null || finalName == null){
            log.error("Couldn't create http request or resolve finalName for URL: {} and finalName: {}", fileUrl, finalName);
            return new URLDownloadResult(fileUrl, "", false, status, written, 0, "Error creating request", started, Instant.now());
        }
        return downloadFile(client, fileUrl, outDir, req, status, finalName, written, started);
    }

    private static URLDownloadResult downloadFile(HttpClient client, String fileUrl, Path outDir, HttpRequest req, int status, String finalName, long written, Instant started) {
        try{
            Instant downloadStart = Instant.now();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            status = resp.statusCode();
            if (status >= 200 && status < 300) { // only 200 response codes are ok
                Path tmp = outDir.resolve(finalName + ".part");// Partially download chunks
                try (InputStream is = resp.body()) {
                    written = Files.copy(is, tmp);
                }
                Files.move(tmp, outDir.resolve(finalName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);// create eventual file upon success
                Instant end = Instant.now();
                long duration = Duration.between(downloadStart, end).toMillis();
                return new URLDownloadResult(fileUrl, finalName, true, status, written, duration, null, started, end);
            } else {
                throw new IOException("HTTP status " + status);
            }
        } catch (MalformedURLException e) {
            log.error("Invalid URL provided: {}", fileUrl, e);
        } catch (Exception e) {Duration.between(started, Instant.now()).toMillis();
            return new URLDownloadResult(fileUrl, "", false, status, written, 0, e.getMessage(), started, Instant.now());
        }
        return new URLDownloadResult(fileUrl, "", false, status, written, 0, "Unknown Error", started, Instant.now());
    }


}