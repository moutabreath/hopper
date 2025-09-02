package tal.hopper.urlDownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tal.hopper.urlDownloader.config_parser.DownloadConfig;
import tal.hopper.urlDownloader.config_parser.DownloadConfigParser;
import tal.hopper.urlDownloader.config_parser.DownloadSettings;

import static org.junit.jupiter.api.Assertions.*;

class DownloadConfigParserTests {

    @Test
    @DisplayName("Should load and parse config.json successfully")
    void testConfigIsLoadedAndParsedCorrectly() {
        // Act
        DownloadConfig config = DownloadConfigParser.getDownloadConfiguration();

        // Assert
        assertNotNull(config, "The configuration object should not be null.");

        // Assert top-level properties
        assertEquals(8, config.urls.size(), "There should be 8 URLs in the list.");
        assertTrue(config.urls.contains("https://example.com/file1.pdf"), "URL list should contain the expected URL.");
        assertEquals(300, config.maxDownloadTimePerUrl, "Max download time should be 300.");
        assertEquals("./downloads", config.outputDirectory, "Output directory should be './downloads'.");
        assertEquals(4, config.maxConcurrentDownloads, "Max concurrent downloads should be 4.");

        // Assert nested downloadSettings
        DownloadSettings settings = config.downloadSettings;
        assertNotNull(settings, "DownloadSettings should not be null.");
        assertEquals(3, settings.retryAttempts, "Retry attempts should be 3.");
        assertEquals(2000, settings.retryDelay, "Retry delay should be 2000.");
    }
}