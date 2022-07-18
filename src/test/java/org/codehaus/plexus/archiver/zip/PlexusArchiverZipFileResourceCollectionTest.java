package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.components.io.functions.ResourceAttributeSupplier;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlexusArchiverZipFileResourceCollectionTest
        extends TestSupport
{

    @Test
    public void testFilesWithIllegalHtmlChars()
        throws Exception
    {
        File testZip = new File( getBasedir(), "src/test/resources/archiveWithIllegalHtmlFileName.zip" );
        Set<String> seen = new HashSet<>();
        seen.add( "AFileThatNeedsHtmlEsc%3F&gt" );
        seen.add( "Afile&lt;Yo&gt;.txt" );
        seen.add( "File With Space.txt" );
        seen.add( "FileWith%.txt" );
        PlexusArchiverZipFileResourceCollection prc = new PlexusArchiverZipFileResourceCollection();
        prc.setFile( testZip );
        final Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            final PlexusIoResource next = entries.next();
            assertTrue( seen.remove( next.getName() ), next.getName() + "was not present" );
            final InputStream contents = next.getContents();
            contents.close();
        }
    }

    @Test
    public void testFileModes()
        throws IOException
    {
        File testZip = new File( getBasedir(), "src/test/resources/zeroFileMode/mixed-file-mode.zip" );
        Map<String, Integer> originalUnixModes = new HashMap<>();
        originalUnixModes.put( "platform-fat", -1 );
        originalUnixModes.put( "zero-unix-mode", 0 );
        // ---xrw-r-- the crazy permissions are on purpose so we don't hit some default value
        originalUnixModes.put( "non-zero-unix-mode", 0164 );
        PlexusArchiverZipFileResourceCollection prc = new PlexusArchiverZipFileResourceCollection();
        prc.setFile( testZip );
        Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            PlexusIoResource entry = entries.next();
            int entryUnixMode = ( (ResourceAttributeSupplier) entry ).getAttributes().getOctalMode();
            int originalUnixMode = originalUnixModes.get( entry.getName() );
            assertEquals( originalUnixMode, entryUnixMode );
        }
    }

}
