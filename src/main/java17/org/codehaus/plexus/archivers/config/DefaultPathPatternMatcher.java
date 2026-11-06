package org.codehaus.plexus.archivers.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.codehaus.plexus.archiver.util.AbstractFileSet;
import org.codehaus.plexus.archivers.config.StandardExcludes;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

class DefaultPathPatternMatcher implements PathPatternMatcher {

    private final String[] includes;
    private String[] excludes = new String[0];
    private Collection<StandardExcludes> standardExcludes = List.of();
    private CaseSensitivity caseSensitivity;
    private FileSelector[] fileSelectors = new FileSelector[0];
    

    private DefaultPathPatternMatcher(String[] includes) {
        this.includes = includes.clone();
    }

    static DefaultPathPatternMatcher includes(String... includes) {
        return new DefaultPathPatternMatcher(includes);
    }

    @Override
    public DefaultPathPatternMatcher excludes(String... excludes) {
        this.excludes = excludes.clone();
        return this;
    }

    @Override
    public PathPatternMatcher caseSensitive(CaseSensitivity caseSensitivity) {
		this.caseSensitivity = caseSensitivity;
		return this;
    }

	@Override
	public PathPatternMatcher standardExcludes(StandardExcludes.ExcludeMode mode) {
		this.standardExcludes = List.of(mode);
		return this;
	}

	@Override
	public PathPatternMatcher standardExcludes(StandardExcludes.ExcludeGroup... groups) {
		this.standardExcludes = Arrays.asList(groups);
		return this;
	}

    DefaultPathPatternMatcher selectedBy(FileSelector... fileSelectors) {
        this.fileSelectors = fileSelectors.clone();
        return this;
    }

    @Override
    public boolean matches(Path path) {
    	throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean matches(ArchiveEntry archiveEntry) {
    	throw new UnsupportedOperationException();
    }

    void applyTo(AbstractFileSet<?> fileSet) {
        fileSet.setIncludes(includes.clone());
        fileSet.setExcludes(excludes.clone());
        fileSet.setCaseSensitive(CaseSensitivities.resolve(caseSensitivity));
        fileSet.setFileSelectors(fileSelectors.clone());
        
        if (standardExcludes.contains(StandardExcludes.NONE)) {
        	fileSet.usingDefaultExcludes(false);
        } else if (standardExcludes.contains(StandardExcludes.PLEXUS)) {
        	fileSet.usingDefaultExcludes(true);
        }
    }
}
