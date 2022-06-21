package org.codehaus.plexus.archiver;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test support class.
 */
public abstract class TestSupport
        extends PlexusTestCase
{
    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        configuration.setAutoWiring( true ).setClassPathScanning( PlexusConstants.SCANNING_INDEX );
    }
}
