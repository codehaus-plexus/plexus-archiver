package org.codehaus.plexus.archiver.zip;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.components.io.functions.SymlinkDestinationSupplier;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoURLResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlexusIoZipFileResourceCollectionTest
        extends TestSupport
{

    @Test
    public void testNamelessRootFolder()
        throws Exception
    {
        PlexusIoZipFileResourceCollection resourceCollection = new PlexusIoZipFileResourceCollection();
        resourceCollection.setFile( getTestFile( "src/test/jars/namelessrootfolder.jar" ) );
        Iterator iterator = resourceCollection.getResources();
        PlexusIoURLResource entry = (PlexusIoURLResource) iterator.next();
        assertEquals( "/dummy.txt", entry.getName() );
        final URL url = entry.getURL();
        BufferedReader d = new BufferedReader( new InputStreamReader( entry.getContents() ) );
        assertEquals( "dummy content", d.readLine() );
    }

    @Test
    public void testDescriptionForError()
        throws Exception
    {
        PlexusIoZipFileResourceCollection resourceCollection = new PlexusIoZipFileResourceCollection();
        resourceCollection.setFile( getTestFile( "src/test/jars/namelessrootfolder.jar" ) );
        Iterator<PlexusIoResource> iterator = resourceCollection.getResources();
        PlexusIoURLResource entry = (PlexusIoURLResource) iterator.next();
        final URL url = entry.getURL();
        String descriptionForError = entry.getDescriptionForError( url );
        assertTrue( descriptionForError.endsWith( "namelessrootfolder.jar!//dummy.txt" ) );
    }

    @Test
    public void testFilesWithIllegalHtmlChars()
        throws Exception
    {
        File testZip = new File( getBasedir(), "src/test/resources/bogusManifest.zip" );
        PlexusIoZipFileResourceCollection prc = new PlexusIoZipFileResourceCollection();
        prc.setFile( testZip );
        final Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            final PlexusIoResource next = entries.next();
            if ( next.getName().endsWith( "MANIFEST.MF" ) )
            {
                final InputStream contents1 = next.getContents();
                final String manifest = IOUtils.toString( contents1, "UTF-8" );
                assertTrue( manifest.contains( "bogs=fus" ) );
                contents1.close();
            }

        }
    }

    @Test
    public void testFilesThatAreNotThere()
        throws Exception
    {
        File testZip = new File( getBasedir(), "src/test/resources/archiveWithIllegalHtmlFileName.zip" );
        Set<String> seen = new HashSet<>();
        seen.add( "AFileThatNeedsHtmlEsc%3F&gt" );
        seen.add( "Afile&lt;Yo&gt;.txt" );
        seen.add( "File With Space.txt" );
        seen.add( "FileWith%.txt" );
        PlexusIoZipFileResourceCollection prc = new PlexusIoZipFileResourceCollection();
        prc.setFile( testZip );
        final Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            final PlexusIoResource next = entries.next();
            assertTrue( seen.remove( next.getName() ), next.getName() + "was not present" );
            final URL url = next.getURL();
            final InputStream contents = next.getContents();
            contents.close();
        }
    }

    @Test
    public void testSymlinkEntries()
        throws Exception
    {
        File testZip = new File( getBasedir(), "src/test/resources/symlinks/symlinks.zip" );
        Map<String, String> symLinks = new HashMap<>();
        symLinks.put( "symDir", "targetDir/" );
        symLinks.put( "symLinkToDirOnTheOutside", "../dirOnTheOutside/" );
        symLinks.put( "symLinkToTheOutside", "../onTheOutside.txt" );
        symLinks.put( "symR", "fileR.txt" );
        symLinks.put( "symW", "fileW.txt" );
        symLinks.put( "symX", "fileX.txt" );
        PlexusIoZipFileResourceCollection prc = new PlexusIoZipFileResourceCollection();
        prc.setFile( testZip );
        final Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            final PlexusIoResource next = entries.next();
            String symLinkTarget = symLinks.remove( next.getName() );
            if ( symLinkTarget != null )
            {
                assertTrue( next.isSymbolicLink(), next.getName() + " must be symlink" );
                assertTrue( next instanceof SymlinkDestinationSupplier );
                assertEquals( symLinkTarget,
                              ( (SymlinkDestinationSupplier) next ).getSymlinkDestination() );
            }
            else
            {
                assertFalse( next.isSymbolicLink(), next.getName() + " must not be symlink" );
            }
        }

        assertTrue( symLinks.isEmpty() );
    }

    @Test
    public void testUnarchiveUnicodePathExtra()
        throws Exception
    {
        PlexusIoZipFileResourceCollection prc = new PlexusIoZipFileResourceCollection();
        prc.setFile( getTestFile( "src/test/resources/unicodePathExtra/efsclear.zip" ) );
        Set<String> names = new HashSet<>();
        final Iterator<PlexusIoResource> entries = prc.getEntries();
        while ( entries.hasNext() )
        {
            final PlexusIoResource next = entries.next();
            names.add(next.getName());
        }
        // a Unicode Path extra field should only be used when its CRC matches the header file name
        assertEquals( new HashSet<>( Arrays.asList( "nameonly-name", "goodextra-extra", "badextra-name" ) ), names,
                      "should use good extra fields but not bad ones" );
    }

}
