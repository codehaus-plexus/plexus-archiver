package org.codehaus.plexus.archiver.manager;

import java.util.Map;

public class DefaultResourceCollectionRegistry extends AbstractResourceCollectionRegistry {

    public DefaultResourceCollectionRegistry(
            Map<String, PlexusIoResourceCollectionFactory> plexusIoResourceCollections) {
        super(plexusIoResourceCollections);
    }
}
