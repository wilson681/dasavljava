package utility;

import java.io.File;
import java.net.URISyntaxException;
/*
 * Locates data files regardless of where the program is started.
 * It searches upward from the compiled class location until the file is found.
 *
 * @author All
 */
public final class DataFileLocator {

    // Utility class should not be instantiated.
    private DataFileLocator() {
    }

     /*
     * Searches parent folders for the given relative path.
     * Returns null if the file cannot be found.
     */
    public static File locate(Class<?> anchorClass, String relativePath) {
        try {
            File location = new File(anchorClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = location.isFile() ? location.getParentFile() : location;
            while (dir != null) {
                File candidate = new File(dir, relativePath);
                if (candidate.exists()) {
                    return candidate;
                }
                dir = dir.getParentFile();
            }
        } catch (URISyntaxException | NullPointerException e) {
            // File location could not be resolved.
        }
        return null;
    }
}
