/*
 * Copyright  2001,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.codehaus.plexus.archiver.manager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

/**
 * @author dantran
 */
@Singleton
@Named
public class DefaultArchiverManager extends AbstractArchiverManager {

    @Inject
    public DefaultArchiverManager(
            Map<String, Provider<Archiver>> archivers,
            Map<String, Provider<UnArchiver>> unArchivers,
            Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections) {
        super(archivers(archivers), unarchivers(unArchivers), plexusIoResourceCollections(plexusIoResourceCollections));
    }

    private static Map<String, ArchiverFactory> archivers(Map<String, Provider<Archiver>> archivers) {
        return archivers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> new CdiArchiverFactory(entry.getValue())));
    }

    private static Map<String, UnArchiverFactory> unarchivers(Map<String, Provider<UnArchiver>> unArchivers) {
        return unArchivers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> new CdiUnArchiverFactory(entry.getValue())));
    }

    private static Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections(
            Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections) {
        return plexusIoResourceCollections.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> new CdiPlexusIoResourceCollectionFactory(entry.getValue())));
    }
}
