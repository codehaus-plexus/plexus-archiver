package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class SymlinkTest
    extends TestSupport
{

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testSymlinkDir()
        throws IOException
    {
        File dummyContent = getTestFile( "src/test/resources/symlinks/src/symDir" );
        assertTrue( dummyContent.isDirectory() );
        assertTrue( Files.isSymbolicLink( dummyContent.toPath() ) );
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testSymlinkDirWithSlash()
        throws IOException
    {
        File dummyContent = getTestFile( "src/test/resources/symlinks/src/symDir/" );
        assertTrue( dummyContent.isDirectory() );
        assertTrue( Files.isSymbolicLink( dummyContent.toPath() ) );
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testSymlinkFile()
    {
        File dummyContent = getTestFile( "src/test/resources/symlinks/src/symR" );
        assertFalse( dummyContent.isDirectory() );
        assertTrue( Files.isSymbolicLink( dummyContent.toPath() ) );
    }

    @Test
    public void testSymlinkTar()
        throws Exception
    {
        TarArchiver archiver = (TarArchiver) lookup( Archiver.class, "tar" );
        archiver.setLongfile( TarLongFileMode.posix );

        File dummyContent = getTestFile( "src/test/resources/symlinks/src" );
        archiver.addDirectory( dummyContent );
        final File archiveFile = new File( "target/output/symlinks.tar" );
        archiver.setDestFile( archiveFile );
        archiver.createArchive();
        File output = getTestFile( "target/output/untaredSymlinks" );
        output.mkdirs();
        TarUnArchiver unarchiver = (TarUnArchiver) lookup( UnArchiver.class, "tar" );
        unarchiver.setSourceFile( archiveFile );
        unarchiver.setDestFile( output );
        unarchiver.extract();
    }

    @Test
    public void testSymlinkZip()
        throws Exception
    {
        ZipArchiver archiver = (ZipArchiver) lookup( Archiver.class, "zip" );

        File dummyContent = getTestFile( "src/test/resources/symlinks/src" );
        archiver.addDirectory( dummyContent );
        final File archiveFile = new File( "target/output/symlinks.zip" );
        archiveFile.delete();
        archiver.setDestFile( archiveFile );
        archiver.createArchive();

        File output = getTestFile( "target/output/unzippedSymlinks" );
        output.mkdirs();
        ZipUnArchiver unarchiver = (ZipUnArchiver) lookup( UnArchiver.class, "zip" );
        unarchiver.setSourceFile( archiveFile );
        unarchiver.setDestFile( output );
        unarchiver.extract();
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testSymlinkDirArchiver()
        throws Exception
    {
        DirectoryArchiver archiver = (DirectoryArchiver) lookup( Archiver.class, "dir" );

        File dummyContent = getTestFile( "src/test/resources/symlinks/src" );
        archiver.addDirectory( dummyContent );
        final File archiveFile = new File( "target/output/dirarchiver-symlink" );
        archiveFile.mkdirs();
        archiver.setDestFile( archiveFile );
        archiver.addSymlink( "target/output/dirarchiver-symlink/aNewDir/symlink", "." );

        archiver.createArchive();

        File symbolicLink = new File( "target/output/dirarchiver-symlink/symR" );
        assertTrue( Files.isSymbolicLink( symbolicLink.toPath() ) );

        symbolicLink = new File( "target/output/dirarchiver-symlink/aDirWithALink/backOutsideToFileX" );
        assertTrue( Files.isSymbolicLink( symbolicLink.toPath() ) );
    }

}
