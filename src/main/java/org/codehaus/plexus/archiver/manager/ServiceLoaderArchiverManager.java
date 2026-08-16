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
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.codehaus.plexus.archivers.spi.ArchiverProvider;
import org.codehaus.plexus.archivers.spi.PlexusIoResourceCollectionProvider;
import org.codehaus.plexus.archivers.spi.UnArchiverProvider;

public class ServiceLoaderArchiverManager extends AbstractArchiverManager {

    public ServiceLoaderArchiverManager() {
        super(archivers(), unarchivers(), plexusIoResourceCollections());
    }

    private static Map<String, ArchiverFactory> archivers() {
        return ServiceLoader.load(ArchiverProvider.class).stream()
                .map(Provider::get)
                .collect(Collectors.toMap(ArchiverProvider::getName, provider -> provider::newArchiver));
    }

    private static Map<String, UnArchiverFactory> unarchivers() {
        return ServiceLoader.load(UnArchiverProvider.class).stream()
                .map(Provider::get)
                .collect(Collectors.toMap(UnArchiverProvider::getName, provider -> provider::newUnarchiver));
    }

    private static Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections() {
        return ServiceLoader.load(PlexusIoResourceCollectionProvider.class).stream()
                .map(Provider::get)
                .collect(Collectors.toMap(
                        PlexusIoResourceCollectionProvider::getName,
                        provider -> provider::newPlexusIoResourceCollection));
    }
}
