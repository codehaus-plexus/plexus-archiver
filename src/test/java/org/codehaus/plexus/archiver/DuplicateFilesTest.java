package org.codehaus.plexus.archiver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Erik Engstrom
 */
public class DuplicateFilesTest
    extends TestSupport
{

    private static final File file1 = getTestFile( "src/test/resources/group-writable/foo.txt" );

    private static final File file2 = getTestFile( "src/test/resources/world-writable/foo.txt" );

    private static final File destination = getTestFile( "target/output/duplicateFiles" );


    @Test
    public void testZipArchiver()
        throws Exception
    {
        Archiver archiver = lookup( Archiver.class, "zip" );
        archiver.setDuplicateBehavior( Archiver.DUPLICATES_SKIP );

        File archive = createArchive( archiver, "zip" );

        org.apache.commons.compress.archivers.zip.ZipFile zf =
            new org.apache.commons.compress.archivers.zip.ZipFile( archive );

        Enumeration<ZipArchiveEntry> e = zf.getEntries();
        int entryCount = 0;
        while ( e.hasMoreElements() )
        {
            ZipArchiveEntry entry = e.nextElement();
            System.out.println( entry.getName() );
            entryCount++;
        }
        zf.close();

        // Zip file should have 2 entries, 1 for the directory and one for foo.txt
        assertEquals( 2, entryCount );
        testArchive( archive, "zip" );
    }

    @Test
    public void testDirArchiver()
        throws Exception
    {
        Archiver archiver = lookup( Archiver.class, "dir" );
        createArchive( archiver, "dir" );
        testFinalFile( "target/output/duplicateFiles.dir/duplicateFiles/foo.txt" );

    }

    @Test
    public void testTarArchiver()
        throws Exception
    {
        TarArchiver archiver = (TarArchiver) lookup( Archiver.class, "tar" );
        archiver.setLongfile( TarLongFileMode.posix );
        archiver.setDuplicateBehavior( Archiver.DUPLICATES_SKIP );

        File archive = createArchive( archiver, "tar" );
        TarArchiveInputStream tis;

        tis = new TarArchiveInputStream( new BufferedInputStream( Files.newInputStream( archive.toPath() ) ) );
        int entryCount = 0;
        while ( ( tis.getNextEntry() ) != null )
        {
            entryCount++;
        }
        assertEquals( 1, entryCount );
        testArchive( archive, "tar" );
        tis.close();
    }

    private File createArchive( Archiver archiver, String outputFileExt )
        throws Exception
    {
        archiver.addFile( file1, "duplicateFiles/foo.txt" );
        archiver.addFile( file2, "duplicateFiles/foo.txt" );

        // delete it if it exists to ensure it is actually empty
        if ( destination.exists() )
        {
            destination.delete();
        }

        File archive = getTestFile( "target/output/duplicateFiles." + outputFileExt );
        if ( archive.exists() )
        {
            if ( archive.isDirectory() )
            {
                FileUtils.deleteDirectory( archive );
            }
            else
            {
                archive.delete();
            }
        }

        archiver.setDestFile( archive );
        archiver.createArchive();
        return archive;
    }

    private void testArchive( File archive, String role )
        throws Exception
    {
        // Check the content of the archive by extracting it

        UnArchiver unArchiver = lookup( UnArchiver.class, role );
        unArchiver.setSourceFile( archive );

        unArchiver.setDestDirectory( getTestFile( "target/output/" ) );
        unArchiver.extract();

        assertTrue( destination.exists() );
        assertTrue( destination.isDirectory() );
        testFinalFile( "target/output/duplicateFiles/foo.txt" );
    }

    private void testFinalFile( String path )
        throws Exception
    {
        File outputFile = getTestFile( path );
        assertTrue( outputFile.exists() );
        BufferedReader reader = Files.newBufferedReader( outputFile.toPath(), StandardCharsets.UTF_8 );
        String firstLine = reader.readLine();
        reader.close();
        reader = Files.newBufferedReader( file2.toPath(), StandardCharsets.UTF_8 );
        String expectedFirstLine = reader.readLine();
        reader.close();
        assertEquals( expectedFirstLine, firstLine );
    }

}
