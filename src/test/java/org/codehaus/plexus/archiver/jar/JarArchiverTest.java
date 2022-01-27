package org.codehaus.plexus.archiver.jar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class JarArchiverTest
    extends BaseJarArchiverTest
{

    @Test
    public void testCreateManifestOnlyJar()
        throws IOException, ManifestException, ArchiverException
    {
        File jarFile = File.createTempFile( "JarArchiverTest.", ".jar" );
        jarFile.deleteOnExit();

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile( jarFile );

        Manifest manifest = new Manifest();
        Manifest.Attribute attribute = new Manifest.Attribute( "Main-Class", getClass().getName() );

        manifest.addConfiguredAttribute( attribute );

        archiver.addConfiguredManifest( manifest );

        archiver.createArchive();
    }

    @Test
    public void testNonCompressed()
        throws IOException, ManifestException, ArchiverException
    {
        File jarFile = new File( "target/output/jarArchiveNonCompressed.jar" );

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile( jarFile );
        archiver.setCompress( false );
        archiver.addDirectory( new File( "src/test/resources/mjar179" ) );
        archiver.createArchive();
    }

    @Test
    public void testVeryLargeJar()
        throws IOException, ManifestException, ArchiverException
    {
        // Generate some number of random files that is likely to be
        // two or three times the number of available file handles
        File tmpDir = File.createTempFile( "veryLargeJar", null );
        tmpDir.delete();
        tmpDir.mkdirs();
        Random rand = new Random();
        for ( int i = 0; i < 45000; i++ )
        {
            File f = new File( tmpDir, "file" + i );
            f.deleteOnExit();
            OutputStream out = Files.newOutputStream( f.toPath() );
            byte[] data = new byte[ 512 ]; // 512bytes per file
            rand.nextBytes( data );
            out.write( data );
            out.flush();
            out.close();
        }

        File jarFile = new File( "target/output/veryLargeJar.jar" );

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( tmpDir );
        archiver.createArchive();
    }

    @Test
    public void testExcludingIndexEntries()
            throws IOException, ArchiverException
    {
        Path tmpDir = File.createTempFile( "jarWithIndexList", null ).toPath();
        Files.delete( tmpDir );

        Files.createDirectories( tmpDir.resolve( "dir.jar" ) );
        Files.createFile( tmpDir.resolve( "dir.jar/file" ) );
        Files.createFile( tmpDir.resolve( "file.jar" ) );

        File jarFile = new File( "target/output/jarWithIndexList.jar" );

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( tmpDir.toFile() );
        archiver.setIndex( true );
        archiver.createArchive();

        try ( ZipFile resultingArchive = new ZipFile( jarFile ) )
        {
            ZipEntry indexList = resultingArchive.getEntry( "META-INF/INDEX.LIST" );
            try ( BufferedReader in = new BufferedReader( new InputStreamReader( resultingArchive.getInputStream( indexList ) ) ) )
            {
                for ( String line = in.readLine(); line != null; line = in.readLine() ) {
                    assertNotEquals( "dir.jar", line );
                    assertNotEquals( "file.jar", line );
                }
            }
        }
    }

    @Override
    protected JarArchiver getJarArchiver()
    {
        return new JarArchiver();
    }
}
