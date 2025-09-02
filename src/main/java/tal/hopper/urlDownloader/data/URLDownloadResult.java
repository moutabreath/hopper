package tal.hopper.urlDownloader.data;

import java.time.Instant;

public class URLDownloadResult {
    public final String uri;
    public final String fileName;
    public final boolean success;
    public final int statusCode; // 0 if not applicable
    public final long bytesWritten;
    public final long durationMillis;
    public final String errorMessage; // null if success
    public final Instant startedAt;
    public final Instant completedAt;

    public URLDownloadResult(String uri, String fileName, boolean success, int statusCode,
                             long bytesWritten, long durationMillis, String errorMessage,
                             Instant startedAt, Instant completedAt) {
        this.uri = uri;
        this.fileName = fileName;
        this.success = success;
        this.statusCode = statusCode;
        this.bytesWritten = bytesWritten;
        this.durationMillis = durationMillis;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }
}