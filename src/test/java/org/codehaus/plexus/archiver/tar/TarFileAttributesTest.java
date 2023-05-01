package org.codehaus.plexus.archiver.tar;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.components.io.attributes.AttributeUtils;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class TarFileAttributesTest
        extends TestSupport
{

    @TempDir
    private Path tempDir;

    @Override
    @BeforeEach
    public void setUp()
        throws Exception
    {
        super.setUp();

        System.out.println( "Octal 0660 is decimal " + 0660 );
        System.out.println( "Octal 0644 is decimal " + 0644 );
        System.out.println( "Octal 0440 is decimal " + 0440 );
    }

    private void printTestHeader()
    {
        StackTraceElement e = new Throwable().getStackTrace()[1];
        System.out.println( "\n\nRunning: " + e.getMethodName() + "\n\n" );
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testUseAttributesFromTarArchiveInputInTarArchiverOutput()
        throws Exception
    {
        printTestHeader();

        File tempFile = Files.createTempFile( tempDir, "tar-file-attributes.", ".tmp" ).toFile();

        try ( Writer writer = Files.newBufferedWriter( tempFile.toPath(), StandardCharsets.UTF_8 ) )
        {
            writer.write( "This is a test file." );
        }

        AttributeUtils.chmod(tempFile, 0440);

        TarArchiver tarArchiver = getPosixCompliantTarArchiver();

        File tempTarFile = Files.createTempFile( tempDir, "tar-file.", ".tar" ).toFile();

        tarArchiver.setDestFile( tempTarFile );
        tarArchiver.addFile( tempFile, tempFile.getName(), 0660 );

        tarArchiver.createArchive();

        TarArchiver tarArchiver2 = getPosixCompliantTarArchiver();

        File tempTarFile2 = Files.createTempFile( tempDir, "tar-file.", ".tar" ).toFile();

        tarArchiver2.setDestFile( tempTarFile2 );

        DefaultArchivedFileSet afs = new DefaultArchivedFileSet( tempTarFile );

        System.out.println( "Adding tar archive to new archiver: " + tempTarFile );
        tarArchiver2.addArchivedFileSet( afs );

        tarArchiver2.createArchive();

        // Cut from here, and feed it into a new tar archiver...then unarchive THAT.
        TarUnArchiver tarUnArchiver = (TarUnArchiver) lookup( UnArchiver.class, "tar" );

        File tempTarDir = Files.createTempDirectory( tempDir, "tar-test." ).toFile();

        tarUnArchiver.setDestDirectory( tempTarDir );
        tarUnArchiver.setSourceFile( tempTarFile2 );

        tarUnArchiver.extract();

        PlexusIoResourceAttributes fileAttributes =
            PlexusIoResourceAttributeUtils.getFileAttributes( new File( tempTarDir, tempFile.getName() ) );

        assertEquals( 0660, fileAttributes.getOctalMode() );

    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testUseDetectedFileAttributes()
        throws Exception
    {
        printTestHeader();

        File tempFile = Files.createTempFile( tempDir, "tar-file-attributes.", ".tmp" ).toFile();

        try ( Writer writer = Files.newBufferedWriter( tempFile.toPath(), StandardCharsets.UTF_8 ) )
        {
            writer.write( "This is a test file." );
        }

        AttributeUtils.chmod(tempFile, 0440);

        PlexusIoResourceAttributes fileAttributes = PlexusIoResourceAttributeUtils.getFileAttributes( tempFile );

        assertEquals( 0440, fileAttributes.getOctalMode() );

        TarArchiver tarArchiver = getPosixCompliantTarArchiver();

        File tempTarFile = Files.createTempFile( tempDir, "tar-file.", ".tar" ).toFile();

        tarArchiver.setDestFile( tempTarFile );
        tarArchiver.addFile( tempFile, tempFile.getName() );

        tarArchiver.createArchive();

        TarUnArchiver tarUnArchiver = (TarUnArchiver) lookup( UnArchiver.class, "tar" );

        File tempTarDir = Files.createTempDirectory( tempDir, "tar-test." ).toFile();

        tarUnArchiver.setDestDirectory( tempTarDir );
        tarUnArchiver.setSourceFile( tempTarFile );

        tarUnArchiver.extract();

        fileAttributes = PlexusIoResourceAttributeUtils.getFileAttributes( new File( tempTarDir, tempFile.getName() ) );

        assertEquals( 0440, fileAttributes.getOctalMode() );
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testOverrideDetectedFileAttributes()
        throws Exception
    {
        printTestHeader();

        File tempFile = Files.createTempFile( tempDir, "tar-file-attributes.", ".tmp" ).toFile();

        try ( Writer writer = Files.newBufferedWriter( tempFile.toPath(), StandardCharsets.UTF_8 ) )
        {
            writer.write( "This is a test file." );
        }

        AttributeUtils.chmod(tempFile, 0440);

        TarArchiver tarArchiver = getPosixCompliantTarArchiver();

        File tempTarFile = Files.createTempFile( tempDir, "tar-file.", ".tar" ).toFile();

        tarArchiver.setDestFile( tempTarFile );
        tarArchiver.addFile( tempFile, tempFile.getName(), 0660 );

        tarArchiver.createArchive();

        TarUnArchiver tarUnArchiver = (TarUnArchiver) lookup( UnArchiver.class, "tar" );

        File tempTarDir = Files.createTempDirectory( tempDir, "tar-test." ).toFile();

        tarUnArchiver.setDestDirectory( tempTarDir );
        tarUnArchiver.setSourceFile( tempTarFile );

        tarUnArchiver.extract();

        PlexusIoResourceAttributes fileAttributes =
            PlexusIoResourceAttributeUtils.getFileAttributes( new File( tempTarDir, tempFile.getName() ) );

        assertEquals( 0660, fileAttributes.getOctalMode() );

    }

    private TarArchiver getPosixCompliantTarArchiver() throws Exception
    {
        TarArchiver tarArchiver = (TarArchiver) lookup( Archiver.class, "tar" );
        tarArchiver.setLongfile( TarLongFileMode.posix );
        return tarArchiver;
    }

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testOverrideDetectedFileAttributesUsingFileMode()
        throws Exception
    {
        printTestHeader();
        File tempFile = Files.createTempFile( tempDir, "tar-file-attributes.", ".tmp" ).toFile();

        try ( Writer writer = Files.newBufferedWriter( tempFile.toPath(), StandardCharsets.UTF_8 ) )
        {
            writer.write( "This is a test file." );
        }

        AttributeUtils.chmod(tempFile, 0440);

        TarArchiver tarArchiver = getPosixCompliantTarArchiver();

        File tempTarFile = Files.createTempFile( tempDir, "tar-file.", ".tar" ).toFile();

        tarArchiver.setDestFile( tempTarFile );
        tarArchiver.setFileMode( 0660 );
        tarArchiver.addFile( tempFile, tempFile.getName() );

        tarArchiver.createArchive();

        TarUnArchiver tarUnArchiver = (TarUnArchiver) lookup( UnArchiver.class, "tar" );

        File tempTarDir = Files.createTempDirectory( tempDir, "tar-test." ).toFile();

        tarUnArchiver.setDestDirectory( tempTarDir );
        tarUnArchiver.setSourceFile( tempTarFile );

        tarUnArchiver.extract();

        PlexusIoResourceAttributes fileAttributes =
            PlexusIoResourceAttributeUtils.getFileAttributes( new File( tempTarDir, tempFile.getName() ) );

        assertEquals( 0660, fileAttributes.getOctalMode() );

    }

}
