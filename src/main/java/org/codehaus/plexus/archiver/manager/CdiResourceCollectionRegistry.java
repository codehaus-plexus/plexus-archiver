package org.codehaus.plexus.archiver.manager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;

@Named
public class CdiResourceCollectionRegistry extends AbstractResourceCollectionRegistry {

    @Inject
    public CdiResourceCollectionRegistry(
            Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections) {
        super(plexusIoResourceCollections(plexusIoResourceCollections));
    }

    private static Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections(
            Map<String, Provider<PlexusIoResourceCollection>> plexusIoResourceCollections) {
        return plexusIoResourceCollections.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> new CdiPlexusIoResourceCollectionFactory(entry.getValue())));
    }
}
