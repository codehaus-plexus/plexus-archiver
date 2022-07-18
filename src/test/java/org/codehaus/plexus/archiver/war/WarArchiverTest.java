package org.codehaus.plexus.archiver.war;

import java.io.File;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class WarArchiverTest
        extends TestSupport
{

    private final int expected = 8;

    @Test
    public void testReAddingPlatformSpecificEncoding()
        throws Exception
    {
        WarArchiver archiver = (WarArchiver) lookup( Archiver.class, "war" );
        archiver.setDestFile( new File( getTargetRarFolder(), "test.war" ) );

        File dummyContent = getTestFile( "src/test/resources/folders" );
        archiver.addDirectory( dummyContent );

        assertEquals( expected, count( archiver.getResources() ) ); // I wonder what the first entry is

        File file = getTestFile( "src/test/resources/folders/WEB-INF/web.xml" );
        archiver.setWebxml( file );

        assertEquals( expected, count( archiver.getResources() ) ); // I wonder what the first entry is

        archiver.createArchive();
        assertTrue( new File( getTargetRarFolder(), "test.war" ).exists() );
    }

    @Test
    public void testInfiniteRecursion()
        throws Exception
    {
        WarArchiver archiver = (WarArchiver) lookup( Archiver.class, "war" );
        archiver.setDestFile( new File( getTargetRarFolder(), "test.war" ) );

        // Easy to produce infinite recursion if you just add existing files again and again
        File dummyContent = getTestFile( "src/test/resources/folders", "File.txt" );
        final int INFINITY = 10;
        for ( int i = 0; i < INFINITY; i++ )
        {
            archiver.addFile( dummyContent, "testZ" );
        }
        assertEquals( 1, count( archiver.getResources() ) ); // I wonder what the first entry is
    }

    public File getTargetRarFolder()
    {
        return new File( getBasedir(), "/target/wartest/" );
    }

    @Override
    @BeforeEach
    public void setUp()
        throws Exception
    {
        super.setUp();
        // clean output directory and re create it
        if ( getTargetRarFolder().exists() )
        {
            FileUtils.deleteDirectory( getTargetRarFolder() );
        }
    }

    private int count( ResourceIterator resourceIterator )
    {
        int i = 0;
        while ( resourceIterator.hasNext() )
        {
            i++;
            ArchiveEntry next = resourceIterator.next();
            System.out.print( next.getMode() );
            System.out.println( next.getName() );
        }
        return i;
    }

}
