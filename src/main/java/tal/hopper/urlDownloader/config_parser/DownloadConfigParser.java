package tal.hopper.urlDownloader.config_parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DownloadConfigParser {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger log = LoggerFactory.getLogger(DownloadConfigParser.class);
    private static final String CONFIG_FILE_NAME = "config.json";

    /**
     * Loads and parses the download configuration from the {@code config.json} file located in the classpath resources.
     *
     * @return A {@link DownloadConfig} object populated with the settings from the JSON file, or {@code null} if loading fails.
     */
    public static DownloadConfig getDownloadConfiguration(String configFileName) {
        String resolveConfigFileName = CONFIG_FILE_NAME;
        if (configFileName != null && !configFileName.isBlank()){
            resolveConfigFileName = configFileName;
        }
        // First, get the stream from the classpath.
        InputStream inputStream = DownloadConfigParser.class.getClassLoader().getResourceAsStream(resolveConfigFileName);

        // Check for null *before* trying to use it in a reader.
        if (inputStream == null) {
            log.error("Cannot find configuration file '{}' on the classpath. Make sure it's in 'src/main/resources'.", CONFIG_FILE_NAME);
            return null;
        }

        // Now that we know the stream is valid, use try-with-resources to ensure it's closed.
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            DownloadConfig config = GSON.fromJson(reader, DownloadConfig.class);
            log.info("Successfully loaded configuration from '{}'.", CONFIG_FILE_NAME);
            return config;
        } catch (Exception ex) {
            log.error("Error loading or parsing config file from resource '{}'", CONFIG_FILE_NAME, ex);
            return null;
        }
    }
}