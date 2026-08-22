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
import org.codehaus.plexus.archiver.ArchiverConfigurer;
import org.codehaus.plexus.archiver.PlexusIoResourceCollectionConfigurer;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.UnArchiverConfigurer;
import org.codehaus.plexus.archivers.spi.ArchiverProvider;
import org.codehaus.plexus.archivers.spi.PlexusIoResourceCollectionProvider;
import org.codehaus.plexus.archivers.spi.UnArchiverProvider;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

public class ServiceLoaderArchiverManager extends AbstractArchiverManager {

    public ServiceLoaderArchiverManager() {
        super(archivers(), unarchivers(), plexusIoResourceCollections());
    }

    private static Map<String, ArchiverFactory> archivers() {
        return StreamSupport.stream(ServiceLoader.load(ArchiverProvider.class).spliterator(), false)
                .collect(Collectors.toMap(ArchiverProvider::getName, ServiceLoaderArchiverManager::toArchiverFactory));
    }

    private static Map<String, UnArchiverFactory> unarchivers() {
        return StreamSupport.stream(ServiceLoader.load(UnArchiverProvider.class).spliterator(), false)
                .collect(Collectors.toMap(UnArchiverProvider::getName, ServiceLoaderArchiverManager::toUnArchiverFactory));
    }

    private static Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections() {
        return StreamSupport.stream(
                        ServiceLoader.load(PlexusIoResourceCollectionProvider.class)
                                .spliterator(),
                        false)
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
