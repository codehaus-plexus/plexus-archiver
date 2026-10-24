package org.codehaus.plexus.archivers.config;

import java.util.Collection;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

public interface ResourceEmitConfigurer {

	void prefix(String prefix);

	void setEmptyDirectoryHandling(EmptyDirectoryHandling handling);

	void setFileMappers(Collection<FileMapper> fileMappers);

	void setStreamTransformer(InputStreamTransformer transformer);
}
