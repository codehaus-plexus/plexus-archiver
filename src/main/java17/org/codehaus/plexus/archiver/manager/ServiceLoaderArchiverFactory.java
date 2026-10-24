package org.codehaus.plexus.archiver.manager;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archivers.config.ArchiverConfigurer;;
import org.codehaus.plexus.archivers.provider.ArchiverProvider;

class ServiceLoaderArchiverFactory implements ArchiverFactory {

	private final ArchiverProvider provider;
	private final Map<String, PlexusIoResourceCollectionFactory> resourceCollectionFactories;
	
	ServiceLoaderArchiverFactory(ArchiverProvider provider, Map<String, PlexusIoResourceCollectionFactory> resourceCollectionFactories) {
		this.provider = provider;
		this.resourceCollectionFactories = resourceCollectionFactories;
	}
	
	@Override
	public Archiver create() {
		Archiver archiver = provider.newArchiver(c -> {});

		if(archiver instanceof AbstractArchiver aa) {
			aa.setResourceCollectionRegistry(new DefaultResourceCollectionRegistry(resourceCollectionFactories));
		}
		return archiver;
	}
	
	@Override
    public Archiver create(Path path, Consumer<ArchiverConfigurer> configurer) {
		Archiver archiver = provider.newArchiver(configurer);
		archiver.setDestFile(path.toFile());
		
		if(archiver instanceof AbstractArchiver aa) {
			aa.setResourceCollectionRegistry(new DefaultResourceCollectionRegistry(resourceCollectionFactories));
		}
    	return archiver;
    }
}
