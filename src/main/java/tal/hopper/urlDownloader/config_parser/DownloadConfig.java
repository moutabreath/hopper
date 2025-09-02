package tal.hopper.urlDownloader.config_parser;
import java.util.List;

// Main configuration class
public class DownloadConfig {
    public List<String> urls;
    public int maxDownloadTimePerUrl;
    public String outputDirectory;
    public int maxConcurrentDownloads;
}

