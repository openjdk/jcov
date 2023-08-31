package openjdk.codetools.jcov.report.filter;

import java.util.Set;

/**
 * A source filter which is also aware of what files need to be included.
 */
public interface SourceFileFilter extends SourceFilter {
    Set<String> files();
}
