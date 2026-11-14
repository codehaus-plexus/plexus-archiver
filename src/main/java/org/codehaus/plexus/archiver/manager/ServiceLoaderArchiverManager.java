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

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archivers.spi.ArchiverProvider;
import org.codehaus.plexus.archivers.spi.PlexusIoResourceCollectionProvider;
import org.codehaus.plexus.archivers.spi.UnArchiverProvider;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

public class ServiceLoaderArchiverManager extends AbstractArchiverManager {

    public ServiceLoaderArchiverManager() {
        super(archivers(), unarchivers(), plexusIoResourceCollections());
    }

    private static Map<String, Supplier<Archiver>> archivers() {
        Map<String, Supplier<Archiver>> result = new HashMap<>();
        for (ArchiverProvider provider : ServiceLoader.load(ArchiverProvider.class)) {
            result.put(provider.getName(), provider::newArchiver);
        }
        return result;
    }

    private static Map<String, Supplier<UnArchiver>> unarchivers() {
        Map<String, Supplier<UnArchiver>> result = new HashMap<>();
        for (UnArchiverProvider provider : ServiceLoader.load(UnArchiverProvider.class)) {
            result.put(provider.getName(), provider::newUnarchiver);
        }
        return result;
    }

    private static Map<String, Supplier<PlexusIoResourceCollection>> plexusIoResourceCollections() {
        Map<String, Supplier<PlexusIoResourceCollection>> result = new HashMap<>();
        for (PlexusIoResourceCollectionProvider provider :
                ServiceLoader.load(PlexusIoResourceCollectionProvider.class)) {
            result.put(provider.getName(), provider::newPlexusIoResourceCollection);
        }
        return result;
    }
}
