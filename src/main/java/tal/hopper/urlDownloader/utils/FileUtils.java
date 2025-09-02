package tal.hopper.urlDownloader.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    public static String resolveFileName(Path dir, String desired) {
        Path p = dir.resolve(desired);
        if (!Files.exists(p)) return desired;
        String name = desired;
        String stem = desired;
        String ext = "";
        int dot = desired.lastIndexOf('.');
        if (dot > 0) { stem = desired.substring(0, dot); ext = desired.substring(dot); }
        int i = 2;
        while (Files.exists(p)) {
            name = stem + "-" + i + ext;
            p = dir.resolve(name);
            i++;
        }
        return name;
    }


    public static String deriveFileName(String path) {
        String base = (path == null || path.isBlank()) ? "index" : path.substring(path.lastIndexOf('/') + 1);
        if (base.isBlank()) base = "index";
        // sanitize
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.equals("_") || base.equals(".")) base = "index";
        return base + (base.contains(".") ? "" : ".bin");
    }

}
