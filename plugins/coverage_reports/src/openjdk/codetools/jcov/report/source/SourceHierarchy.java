package openjdk.codetools.jcov.report.source;

import java.io.IOException;
import java.util.List;

/**
 * An abstraction for source hierarchy.
 */
public interface SourceHierarchy {
    /**
     * Delivers the file source code.
     * @param file - file name within the source hierarchy.
     */
    List<String> readFile(String file) throws IOException;

    /**
     * Maps a file name (as present in source) to a class file name.
     * Example: <code>src/main/java/my/company/product/Main.hava</code> to <code>my/company/product/Main.hava</code>.
     * @param file - file name within the source hierarchy.
     */
    String toClass(String file);
}
