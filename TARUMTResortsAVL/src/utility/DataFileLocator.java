package utility;

import java.io.File;
import java.net.URISyntaxException;

/**
 * DataFileLocator.java
 * Utility class — locates where the data files under data/ actually sit on
 * disk.
 *
 * Notes:
 * - Contains only static methods with no state, following the Utility class
 *   convention
 * - A path like "data/members.txt" can't be handed straight to FileReader —
 *   that's relative to the working directory the program was launched from,
 *   and NetBeans, VSCode, and the command line each default to a different
 *   one, so the same code can fail to find the file depending on how it's
 *   started
 * - Instead, this walks upward from where anchorClass's compiled .class file
 *   actually sits on disk (always somewhere inside the project folder,
 *   whether that's NetBeans' build/classes, VSCode's bin, or a packaged
 *   dist/xxx.jar), one directory at a time, until it finds a level where the
 *   relative path really exists — so it never matters which working
 *   directory the program was launched from
 */
public final class DataFileLocator {

    private DataFileLocator() {
        // Prevents external instantiation; a pure static utility class.
    }

    /**
     * Starting from where anchorClass actually sits on disk, walks up
     * through parent directories to locate the file at relativePath.
     * @param anchorClass the class used to locate where the program is
     *        actually running on disk (usually the caller's own .class)
     * @param relativePath path relative to the project root, e.g.
     *        "data/members.txt"
     * @return the File if found; null if not found, left for the caller to
     *         handle
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
            // Couldn't resolve the actual disk location; left for the
            // caller to handle (usually logging a warning and keeping the
            // container empty).
        }
        return null;
    }
}
