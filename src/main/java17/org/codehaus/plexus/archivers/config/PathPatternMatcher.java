package org.codehaus.plexus.archivers.config;

import java.nio.file.PathMatcher;

public interface PathPatternMatcher extends PathMatcher, ArchiveEntryMatcher {

    static PathPatternMatcher includes(String... includes) {
        return DefaultPathPatternMatcher.includes(includes);
    }

    PathPatternMatcher excludes(String... excludes);

    PathPatternMatcher standardExcludes(StandardExcludes.ExcludeMode mode);

    PathPatternMatcher standardExcludes(StandardExcludes.ExcludeGroup... groups);

    PathPatternMatcher caseSensitive(CaseSensitivity caseSensitivity);
    
    
}
