package group11.util;

// ResourceUtil.java
import java.io.*;
import java.net.URL;
import java.nio.file.*;

//https://chatgpt.com/share/68f3eb7a-ea8c-8007-a3f8-b1635d62b988
public final class ResourceUtil {
    private ResourceUtil() {}

    /** Copies a classpath resource to a temp file and returns its Path. */
    public static Path extractResourceToTemp(String resourcePath, String prefix, String suffix) throws IOException {
        URL url = ResourceUtil.class.getResource(resourcePath);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        Path tmp = Files.createTempFile(prefix, suffix);
        tmp.toFile().deleteOnExit();

        try (InputStream in = ResourceUtil.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource stream is null: " + resourcePath);
            }
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp;
    }
}