package org.codehaus.plexus.archiver;

import org.codehaus.plexus.components.io.resources.PlexusIoResource;

/**
 * overridable hook to provide an alternative date when adding an entry in an archive<BR/>
 * instead of relying on default <code>getLastModifed()</code>.
 *
 * <p>
 * This can be used typically for reproducible build: put date ${env.SOURCE_DATE_EPOCH}
 * instead of local filesystem date.
 *
 * <p>
 * Can be implemented also at scm layer, for example: relying on git committed date
 * for git resources.
 */
public abstract class ArchiveEntryDateProvider
{

    /**
     * @param resource
     * @return the expected date to use when adding <code>resource</code> entry in an archive
     */
    public abstract long getEntryArchiveDate( PlexusIoResource resource );

}
