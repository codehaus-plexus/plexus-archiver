package org.codehaus.plexus.archivers.config;

import org.codehaus.plexus.components.io.fileselectors.FileInfo;

public interface FileInfoMatcher {

	boolean matches(FileInfo fileInfo);
}
