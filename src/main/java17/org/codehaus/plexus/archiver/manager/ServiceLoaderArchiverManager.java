/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.codehaus.plexus.archiver.manager;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archivers.config.ArchiverConfigurer;
import org.codehaus.plexus.archivers.config.PlexusIoResourceCollectionConfigurer;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archivers.config.UnArchiverConfigurer;
import org.codehaus.plexus.archivers.spi.ArchiverProvider;
import org.codehaus.plexus.archivers.spi.PlexusIoResourceCollectionProvider;
import org.codehaus.plexus.archivers.spi.UnArchiverProvider;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

class ServiceLoaderArchiverManager extends AbstractArchiverManager {

    ServiceLoaderArchiverManager() {
        super(archivers(ServiceLoader.load(ArchiverProvider.class)), 
        		unarchivers(ServiceLoader.load(UnArchiverProvider.class)), 
        		plexusIoResourceCollections(ServiceLoader.load(PlexusIoResourceCollectionProvider.class)));
    }

    ServiceLoaderArchiverManager(ClassLoader classLoader) {
        super(archivers(ServiceLoader.load(ArchiverProvider.class, classLoader)), 
        		unarchivers(ServiceLoader.load(UnArchiverProvider.class, classLoader)), 
        		plexusIoResourceCollections(ServiceLoader.load(PlexusIoResourceCollectionProvider.class, classLoader)));
	}

	ServiceLoaderArchiverManager(ModuleLayer moduleLayer) {
		 super(archivers(ServiceLoader.load(moduleLayer, ArchiverProvider.class)), 
	        		unarchivers(ServiceLoader.load(moduleLayer, UnArchiverProvider.class)), 
	        		plexusIoResourceCollections(ServiceLoader.load(moduleLayer, PlexusIoResourceCollectionProvider.class)));
	}

	private static Map<String, ArchiverFactory> archivers(ServiceLoader<ArchiverProvider> serviceLoader) {
        return StreamSupport.stream(serviceLoader.spliterator(), false)
                .collect(Collectors.toMap(ArchiverProvider::getName, ServiceLoaderArchiverManager::toArchiverFactory));
    }

    private static Map<String, UnArchiverFactory> unarchivers(ServiceLoader<UnArchiverProvider> serviceLoader) {
        return StreamSupport.stream(ServiceLoader.load(UnArchiverProvider.class).spliterator(), false)
                .collect(Collectors.toMap(UnArchiverProvider::getName, ServiceLoaderArchiverManager::toUnArchiverFactory));
    }

    private static Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections(ServiceLoader<PlexusIoResourceCollectionProvider> serviceLoader) {
        return StreamSupport.stream(serviceLoader.spliterator(),false)
                .collect(Collectors.toMap(
                        PlexusIoResourceCollectionProvider::getName,
                        ServiceLoaderArchiverManager::toPlexusIoResourceCollectionFactory));
    }
    
    
    private static ArchiverFactory toArchiverFactory(ArchiverProvider provider) {
    	return new ArchiverFactory() {
			
			@Override
			public Archiver create() {
				return create(c -> {});
			}
			
		    public Archiver create(Consumer<ArchiverConfigurer> configurer) {
		    	return provider.newArchiver(configurer);
		    }
		};
    } 

    private static UnArchiverFactory toUnArchiverFactory(UnArchiverProvider provider) {
    	return new UnArchiverFactory() {
			
			@Override
			public UnArchiver create() {
				return create(c -> {});
			}
			
		    public UnArchiver create(Consumer<UnArchiverConfigurer> configurer) {
		    	return provider.newUnArchiver(configurer);
		    }
		};
    } 
    
    private static PlexusIoResourceCollectionFactory toPlexusIoResourceCollectionFactory(PlexusIoResourceCollectionProvider provider) {
    	return new PlexusIoResourceCollectionFactory() {
			
			@Override
			public PlexusIoResourceCollection create() {
				return create(c -> {});
			}
			
		    public PlexusIoResourceCollection create(Consumer<PlexusIoResourceCollectionConfigurer> configurer) {
		    	return provider.newPlexusIoResourceCollection(configurer);
		    }
		};
    } 

}
