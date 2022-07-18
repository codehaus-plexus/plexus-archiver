package org.codehaus.plexus.archiver.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.jupiter.api.Test;

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
    public void testReproducibleBuild()
        throws IOException, ManifestException, ParseException
    {
        String[] tzList = { "America/Managua", "America/New_York", "America/Buenos_Aires", "America/Sao_Paulo",
            "America/Los_Angeles", "Africa/Cairo", "Africa/Lagos", "Africa/Nairobi", "Europe/Lisbon", "Europe/Madrid",
            "Europe/Moscow", "Europe/Oslo", "Australia/Sydney", "Asia/Tokyo", "Asia/Singapore", "Asia/Qatar",
            "Asia/Seoul", "Atlantic/Bermuda", "UTC", "GMT", "Etc/GMT-14" };
        for ( String tzId : tzList )
        {
            // Every single run with different Time Zone should set the same modification time.
            createReproducibleBuild( tzId );
        }
    }

    private void createReproducibleBuild( String timeZoneId )
        throws IOException, ManifestException, ParseException
    {
        final TimeZone defaultTz = TimeZone.getDefault();
        TimeZone.setDefault( TimeZone.getTimeZone( timeZoneId ) );
        try
        {
            String tzName = timeZoneId.substring( timeZoneId.lastIndexOf( '/' ) + 1 );
            Path jarFile = Files.createTempFile( "JarArchiverTest-" + tzName + "-", ".jar" );
            jarFile.toFile().deleteOnExit();

            Manifest manifest = new Manifest();
            Manifest.Attribute attribute = new Manifest.Attribute( "Main-Class", "com.example.app.Main" );
            manifest.addConfiguredAttribute( attribute );

            JarArchiver archiver = getJarArchiver();
            archiver.setDestFile( jarFile.toFile() );
            archiver.addConfiguredManifest( manifest );
            archiver.addDirectory( new File( "src/test/resources/java-classes" ) );

            SimpleDateFormat isoFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssXXX" );
            long parsedTime = isoFormat.parse( "2038-01-19T03:14:08Z" ).getTime();
            FileTime lastModTime = FileTime.fromMillis( parsedTime );

            archiver.configureReproducibleBuild( lastModTime );
            archiver.createArchive();

            // zip 2 seconds precision, normalized to UTC
            long expectedTime = normalizeLastModifiedTime( parsedTime - ( parsedTime % 2000 ) );
            try ( ZipFile zip = new ZipFile( jarFile.toFile() ) )
            {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while ( entries.hasMoreElements() )
                {
                    ZipEntry entry = entries.nextElement();
                    long time = entry.getTime();
                    assertEquals( expectedTime, time, "last modification time does not match" );
                }
            }
        }
        finally
        {
            TimeZone.setDefault( defaultTz );
        }
    }

    @Override
    protected JarArchiver getJarArchiver()
    {
        return new JarArchiver();
    }
}
