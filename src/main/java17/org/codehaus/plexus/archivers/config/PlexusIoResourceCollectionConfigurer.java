package org.codehaus.plexus.archivers.config;

import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

public interface PlexusIoResourceCollectionConfigurer {

	public static PlexusIoResourceCollectionConfigurer of(PlexusIoResourceCollection collection) {
		return new DefaultPlexusIoResourceCollectionConfigurer(collection);
	}
	
	void matcher(PathPatternMatcher matcher);

	void prefix(String prefix);
	
	void setSymbolicLinkHandling(SymbolicLinkHandling handling);

}
