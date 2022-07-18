package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
@Disabled
public class ConcurrentJarCreatorTest
{

    @Test
    public void concurrent()
        throws Exception
    {
        File home = new File( System.getProperty( "user.home" ) );
        File result = new File( home, "multiStream2-parallel.zip" );
        ConcurrentJarCreator zipCreator = new ConcurrentJarCreator( Runtime.getRuntime().availableProcessors() );

        final File file1 = new File( home, "lsrc/plexus" );
        doAddAll( file1.getPath(), zipCreator );

        ZipArchiveOutputStream zos = createZipARchiveOutputStream( result );
        zipCreator.writeTo( zos );
        zos.close();
        System.out.println( "Concurrent:" + zipCreator.getStatisticsMessage() );
    }

    @Test
    public void concurrent2() throws Exception
    {
        concurrent();
    }

    @Test
    @Disabled
    public void classic()
        throws Exception
    {
        long startAt = System.currentTimeMillis();
        File home = new File( System.getProperty( "user.home" ) );
        File result = new File( home, "multiStream2-classic.zip" );

        final File file1 = new File( home, "lsrc/plexus" );
        ZipArchiveOutputStream zos = createZipARchiveOutputStream( result );
        doAddAll( file1.getPath(), zos );
        zos.close();
        System.out.println( "linear:" + ( System.currentTimeMillis() - startAt ) + "ms" );

    }

    private ZipArchiveOutputStream createZipARchiveOutputStream( File result ) throws IOException
    {
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream( result );
        zos.setEncoding( "UTF-8" );
        return zos;
    }

    private void doAddAll( String base, ConcurrentJarCreator mos ) throws IOException
    {

        DirectoryScanner ds = getIncludedFiles( base );

        for ( String fileName : ds.getIncludedFiles() )
        {
            final File file = new File( base, fileName );
            ZipArchiveEntry za = createZipArchiveEntry( file, fileName );

            mos.addArchiveEntry( za, () -> {
                try
                {
                    return file.isFile() ? Files.newInputStream( file.toPath() ) : null;
                }
                catch ( IOException e )
                {
                    throw new UncheckedIOException( e );
                }
            }, true );
        }

    }

    private DirectoryScanner getIncludedFiles( String base )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( base );
        ds.scan();
        return ds;
    }

    private void doAddAll( String base, ZipArchiveOutputStream mos ) throws IOException
    {
        DirectoryScanner ds = getIncludedFiles( base );

        for ( String fileName : ds.getIncludedFiles() )
        {
            final File file = new File( base, fileName );
            ZipArchiveEntry za = createZipArchiveEntry( file, fileName );

            mos.putArchiveEntry( za );
            if ( file.isFile() )
            {
                try (InputStream input = Files.newInputStream( file.toPath() )) {
                    IOUtils.copy( input, mos );
                }
            }
            mos.closeArchiveEntry();
        }

    }

    @SuppressWarnings( "OctalInteger" )
    private ZipArchiveEntry createZipArchiveEntry( File file, String name )
    {
        ZipArchiveEntry za = new ZipArchiveEntry( file, name );
        if ( file.isDirectory() )
        {
            za.setMethod( ZipArchiveEntry.STORED );
            za.setSize( 0 );
            za.setUnixMode( UnixStat.DIR_FLAG | 0664 );
        }
        else
        {
            za.setMethod( ZipArchiveEntry.DEFLATED );
            za.setSize( file.length() );
            za.setUnixMode( UnixStat.FILE_FLAG | 0664 );
        }
        za.setTime( file.lastModified() );
        return za;
    }

}
