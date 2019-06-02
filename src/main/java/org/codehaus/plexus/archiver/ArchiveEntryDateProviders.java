package org.codehaus.plexus.archiver;

import java.time.Instant;

import org.codehaus.plexus.components.io.resources.PlexusIoResource;

/**
 * Implementations of ArchiveEntryDateProvider
 * 
 */
public final class ArchiveEntryDateProviders
{

    public static final ArchiveEntryDateProvider DEFAULT = new DefaultArchiveEntryDateProvider();

    public static final ArchiveEntryDateProvider DEFAULT_VALID_OR_CURRENT = new DefaultValidOrCurrentArchiveEntryDateProvider();

    public static final ArchiveEntryDateProvider DEFAULT_ZIP_ROUND_UP_2SECONDS = new RoundingUpArchiveEntryDateProvider(DEFAULT, 1999);

    /**
     * create a FixedArchiveEntryDateProvider for the specified time in millis
     * @param time time in millis
     * @return nex FixedArchiveEntryDateProvider
     */
    public static ArchiveEntryDateProvider ofFixedTime( long timeMillis )
    {
        return new FixedArchiveEntryDateProvider( timeMillis );
    }
    
    /**
     * create a FixedArchiveEntryDateProvider for the specified datetime or null, in standard ISO format
     * @param date date or null in standard ISO format
     * @return nex FixedArchiveEntryDateProvider
     */
    public static ArchiveEntryDateProvider ofFixedIsoDateTime( String date ) {
        long timeMillis = 0L;
        if ( date != null )
        {
            try
            {
                timeMillis = Instant.parse( date ).toEpochMilli();
            }
            catch( Exception ex )
            {
                // throw new IllegalArgumentException("invalid date format for reproducibleBuild");
                timeMillis = 0L;
            }
        }
        return new FixedArchiveEntryDateProvider( timeMillis );
    }

    /**
     * Default ArchiveEntryDateProvider implementation, using <code>resource.getLastModifiedDate()</code>
     */
    public static class DefaultArchiveEntryDateProvider
        extends ArchiveEntryDateProvider
    {

        @Override
        public long getEntryArchiveDate( PlexusIoResource resource )
        {
            return resource.getLastModified();
        }

    }

    /**
     * ArchiveEntryDateProvider implementation, using <code>resource.getLastModifiedDate()</code>
     * or current time in case of <code>PlexusIoResource.UNKNOWN_MODIFICATION_DATE</code>
     */
    public static class DefaultValidOrCurrentArchiveEntryDateProvider
        extends ArchiveEntryDateProvider
    {

        @Override
        public long getEntryArchiveDate( PlexusIoResource resource )
        {
            long date = resource.getLastModified();
            return ( date != PlexusIoResource.UNKNOWN_MODIFICATION_DATE ) ? date : System.currentTimeMillis();
        }

    }

    /**
     * ArchiveEntryDateProvider implementation using fixed date.
     *
     * <p>
     * Typical usage: for reproducible build: put date ${env.SOURCE_DATE_EPOCH}
     */
    public static class FixedArchiveEntryDateProvider
        extends ArchiveEntryDateProvider
    {

        private final long fixedDate;

        public FixedArchiveEntryDateProvider( long fixedDate )
        {
            this.fixedDate = fixedDate;
        }

        @Override
        public long getEntryArchiveDate( PlexusIoResource resource )
        {
            return fixedDate;
        }

    }

    /**
     * ArchiveEntryDateProvider rounding up with an offset for avoiding rounding up or down with granularity
     *
     * <p>
     * Typical usage:
     * Zip archives store file modification times with a
     * granularity of two seconds, so the times will either be rounded
     * up or down. If you round down, the archive will always seem
     * out-of-date when you rerun the task, so the default is to round
     * up. Rounding up may lead to a different type of problems like
     * JSPs inside a web archive that seem to be slightly more recent
     * than precompiled pages, rendering precompilation useless.
     * plexus-archiver chooses to round up.
     */
    public static class RoundingUpArchiveEntryDateProvider
        extends ArchiveEntryDateProvider
    {
        private final ArchiveEntryDateProvider delegate;

        private final long roundUpMillis;

        public RoundingUpArchiveEntryDateProvider( ArchiveEntryDateProvider delegate, long roundUpMillis )
        {
            this.delegate = delegate;
            this.roundUpMillis = roundUpMillis;
        }

        @Override
        public long getEntryArchiveDate( PlexusIoResource resource )
        {
            long date = delegate.getEntryArchiveDate( resource );
            return date + roundUpMillis;
        }

    }
}
