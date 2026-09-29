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

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import org.codehaus.plexus.archivers.config.PlexusIoResourceCollectionConfigurer;
import org.codehaus.plexus.components.io.resources.PlexusIoArchivedResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

/**
 * Creates configured Plexus IO resource collection instances.
 *
 * @since 5.0.0
 */
@FunctionalInterface
public interface PlexusIoResourceCollectionFactory {
    PlexusIoResourceCollection create();
    
    default PlexusIoResourceCollection create(
            Path path,
            Consumer<PlexusIoResourceCollectionConfigurer> configurer) {

        Objects.requireNonNull(path, "scan");
        Objects.requireNonNull(configurer, "configurer");

        PlexusIoResourceCollection collection = create();
        if(collection instanceof PlexusIoArchivedResourceCollection coll) {
        	coll.setFile(path.toFile());
        }

        configurer.accept(PlexusIoResourceCollectionConfigurer.of(collection));

        return collection;
    }
}
