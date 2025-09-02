package tal.hopper.urlDownloader.config_parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownloadConfigParserTests {

    @Test
    @DisplayName("Should load and parse a custom test config file successfully")
    void testCustomConfigIsLoadedAndParsedCorrectly() {
        // Arrange
        String testConfigFile = "test-config.json";

        // Act
        DownloadConfig config = DownloadConfigParser.getDownloadConfiguration(testConfigFile);

        // Assert
        assertNotNull(config, "The configuration object should not be null.");

        // Assert top-level properties from test-config.json
        assertEquals(2, config.urls.size(), "There should be 2 URLs in the list.");
        assertTrue(config.urls.contains("http://test.com/file1.zip"), "URL list should contain the expected test URL.");
        assertEquals(60, config.maxDownloadTimePerUrl, "Max download time should be 60.");
        assertEquals("./test-downloads", config.outputDirectory, "Output directory should be './test-downloads'.");
        assertEquals(2, config.maxConcurrentDownloads, "Max concurrent downloads should be 2.");

    }

    @Test
    @DisplayName("Should return null for a non-existent config file")
    void testLoadingNonExistentFile() {
        // Arrange
        String nonExistentFile = "non-existent-config.json";

        // Act
        DownloadConfig config = DownloadConfigParser.getDownloadConfiguration(nonExistentFile);

        // Assert
        assertNull(config, "Should return null when the config file does not exist.");
    }
}