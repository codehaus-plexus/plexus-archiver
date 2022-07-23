/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.codehaus.plexus.archiver.tar;

import static org.codehaus.plexus.archiver.util.Streams.bufferedInputStream;
import static org.codehaus.plexus.components.io.resources.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnixStat;
import org.codehaus.plexus.archiver.bzip2.BZip2Compressor;
import org.codehaus.plexus.archiver.exceptions.EmptyArchiveException;
import org.codehaus.plexus.archiver.gzip.GZipCompressor;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.util.Compressor;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;
import org.codehaus.plexus.archiver.zip.ArchiveFileComparator;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * @author Emmanuel Venisse
 */
public class TarArchiverTest
        extends TestSupport
{

    @Test
    @DisabledOnOs( OS.WINDOWS )
    public void testCreateArchiveWithDetectedModes()
        throws Exception
    {

        String[] executablePaths =
        {
            "path/to/executable", "path/to/executable.bat"
        };

        String[] confPaths =
        {
            "path/to/etc/file", "path/to/etc/file2"
        };

        String[] logPaths =
        {
            "path/to/logs/log.txt"
        };

        int exeMode = 0777;
        int confMode = 0600;
        int logMode = 0640;

        File tmpDir = null;
        try
        {
            tmpDir = File.createTempFile( "tbz2-with-chmod.", ".dir" );
            tmpDir.delete();

            tmpDir.mkdirs();

            for ( String executablePath : executablePaths )
            {
                writeFile( tmpDir, executablePath, exeMode );
            }

            for ( String confPath : confPaths )
            {
                writeFile( tmpDir, confPath, confMode );
            }

            for ( String logPath : logPaths )
            {
                writeFile( tmpDir, logPath, logMode );
            }

            {
                Map attributesByPath = PlexusIoResourceAttributeUtils.getFileAttributesByPath( tmpDir );
                for ( String path : executablePaths )
                {
                    PlexusIoResourceAttributes attrs = (PlexusIoResourceAttributes) attributesByPath.get( path );
                    if ( attrs == null )
                    {
                        attrs = (PlexusIoResourceAttributes) attributesByPath.get(
                            new File( tmpDir, path ).getAbsolutePath() );
                    }

                    assertNotNull( attrs );
                    assertEquals( exeMode, attrs.getOctalMode(), "Wrong mode for: " + path );
                }

                for ( String path : confPaths )
                {
                    PlexusIoResourceAttributes attrs = (PlexusIoResourceAttributes) attributesByPath.get( path );
                    if ( attrs == null )
                    {
                        attrs = (PlexusIoResourceAttributes) attributesByPath.get(
                            new File( tmpDir, path ).getAbsolutePath() );
                    }

                    assertNotNull( attrs );
                    assertEquals( confMode, attrs.getOctalMode(), "Wrong mode for: " + path );
                }

                for ( String path : logPaths )
                {
                    PlexusIoResourceAttributes attrs = (PlexusIoResourceAttributes) attributesByPath.get( path );
                    if ( attrs == null )
                    {
                        attrs = (PlexusIoResourceAttributes) attributesByPath.get(
                            new File( tmpDir, path ).getAbsolutePath() );
                    }

                    assertNotNull( attrs );
                    assertEquals( logMode, attrs.getOctalMode(), "Wrong mode for: " + path );
                }
            }

            File tarFile = getTestFile( "target/output/tar-with-modes.tar" );

            TarArchiver archiver = getPosixTarArchiver();
            archiver.setDestFile( tarFile );

            archiver.addDirectory( tmpDir );
            archiver.createArchive();

            assertTrue( tarFile.exists() );

            File tarFile2 = getTestFile( "target/output/tar-with-modes-L2.tar" );

            archiver = getPosixTarArchiver();
            archiver.setDestFile( tarFile2 );

            archiver.addArchivedFileSet( tarFile );
            archiver.createArchive();

            TarFile tf = new TarFile( tarFile2 );

            Map<String, TarArchiveEntry> entriesByPath = new LinkedHashMap<String, TarArchiveEntry>();
            for ( Enumeration e = tf.getEntries(); e.hasMoreElements(); )
            {
                TarArchiveEntry te = (TarArchiveEntry) e.nextElement();
                entriesByPath.put( te.getName(), te );
            }

            for ( String path : executablePaths )
            {
                TarArchiveEntry te = entriesByPath.get( path );

                int mode = te.getMode() & UnixStat.PERM_MASK;

                assertEquals( exeMode, mode, "Wrong mode for: " + path );
            }

            for ( String path : confPaths )
            {
                TarArchiveEntry te = entriesByPath.get( path );

                int mode = te.getMode() & UnixStat.PERM_MASK;

                assertEquals( confMode, mode, "Wrong mode for: " + path );
            }

            for ( String path : logPaths )
            {
                TarArchiveEntry te = entriesByPath.get( path );

                int mode = te.getMode() & UnixStat.PERM_MASK;

                assertEquals( logMode, mode, "Wrong mode for: " + path );
            }
        }
        finally
        {
            if ( tmpDir != null && tmpDir.exists() )
            {
                try
                {
                    FileUtils.forceDelete( tmpDir );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testCreateEmptyArchive()
        throws Exception
    {
        TarArchiver archiver = getPosixTarArchiver();
        archiver.setDestFile( getTestFile( "target/output/empty.tar" ) );
        try
        {
            archiver.createArchive();

            fail( "Creating empty archive should throw EmptyArchiveException" );
        }
        catch ( EmptyArchiveException ignore )
        {
        }
    }

    @Test
    public void testUnicode() throws Exception
    {
        File tmpDir = getTestFile( "src/test/resources/utf8" );
        TarArchiver archiver = getPosixTarArchiver();
        File tarFile = getTestFile( "target/output/tar-with-longFileName.tar" );
        archiver.setDestFile( tarFile );
        archiver.setLongfile( TarLongFileMode.posix ); // Todo: should be gnu. But will fail with high userod
        archiver.addDirectory( tmpDir );
        archiver.createArchive();
        assertTrue( tarFile.exists() );
    }

    private void writeFile( File dir, String fname, int mode )
        throws IOException, ArchiverException
    {
        File file = new File( dir, fname );

        if ( file.getParentFile() != null )
        {
            file.getParentFile().mkdirs();
        }

        try ( Writer writer = Files.newBufferedWriter( file.toPath(), StandardCharsets.UTF_8 ) )
        {
            writer.write( "This is a test file." );
        }

        ArchiveEntryUtils.chmod( file, mode );
    }

    @Test
    public void testCreateArchive()
        throws Exception
    {
        createArchive( 0500,
                       new int[]
                       {
                           0400, 0640, 0664
                       } );
        createArchive( 0500,
                       new int[]
                       {
                           0400, 0640, 0664
                       } );
    }

    public void createArchive( final int directoryMode, final int fileModes[] )
        throws Exception
    {
        int defaultFileMode = fileModes[0];
        int oneFileMode = fileModes[1];
        int twoFileMode = fileModes[2];

        TarArchiver archiver = getPosixTarArchiver();

        archiver.setDirectoryMode( directoryMode );

        archiver.setFileMode( defaultFileMode );

        archiver.addDirectory( getTestFile( "src/main" ) );
        archiver.setFileMode( oneFileMode );

        archiver.addFile( getTestFile( "src/test/resources/manifests/manifest1.mf" ), "one.txt" );
        archiver.addFile( getTestFile( "src/test/resources/manifests/manifest2.mf" ), "two.txt", twoFileMode );
        archiver.setDestFile( getTestFile( "target/output/archive.tar" ) );

        archiver.addSymlink( "link_to_test_destinaton", "../test_destination/" );

        archiver.createArchive();

        TarArchiveInputStream tis;

        tis = new TarArchiveInputStream( bufferedInputStream( Files.newInputStream( archiver.getDestFile().toPath() ) ) );
        TarArchiveEntry te;

        while ( ( te = tis.getNextTarEntry() ) != null )
        {
            if ( te.isDirectory() )
            {
                assertEquals( directoryMode, te.getMode() & UnixStat.PERM_MASK,
                              "un-expected tar-entry mode for [te.name=" + te.getName() + "]" );
            }
            else if ( te.isSymbolicLink() )
            {
                assertEquals( "../test_destination/", te.getLinkName() );
                assertEquals( "link_to_test_destinaton", te.getName() );
                assertEquals( 0640, te.getMode() & UnixStat.PERM_MASK );
            }
            else
            {
                if ( te.getName().equals( "one.txt" ) )
                {
                    assertEquals( oneFileMode, te.getMode() & UnixStat.PERM_MASK );
                }
                else if ( te.getName().equals( "two.txt" ) )
                {
                    assertEquals( twoFileMode, te.getMode() & UnixStat.PERM_MASK );
                }
                else
                {
                    assertEquals( defaultFileMode, te.getMode() & UnixStat.PERM_MASK,
                                  "un-expected tar-entry mode for [te.name=" + te.getName() + "]" );
                }

            }
        }
        IOUtil.close( tis );

    }

    @Test
    public void testCreateArchiveWithJiustASymlink()
        throws Exception
    {
        TarArchiver archiver = getPosixTarArchiver();

        archiver.setDirectoryMode( 0500 );

        archiver.setFileMode( 0400 );

        archiver.setFileMode( 0640 );

        archiver.setDestFile( getTestFile( "target/output/symlinkarchive.tar" ) );

        archiver.addSymlink( "link_to_test_destinaton", "../test_destination/" );

        archiver.createArchive();

        TarArchiveInputStream tis;

        tis = new TarArchiveInputStream( new BufferedInputStream( Files.newInputStream( archiver.getDestFile().toPath() ) ) );
        TarArchiveEntry te;

        while ( ( te = tis.getNextTarEntry() ) != null )
        {
            if ( te.isDirectory() )
            {
                assertEquals( 0500, te.getMode() & UnixStat.PERM_MASK,
                              "un-expected tar-entry mode for [te.name=" + te.getName() + "]" );
            }
            else if ( te.isSymbolicLink() )
            {
                assertEquals( "../test_destination/", te.getLinkName() );
                assertEquals( "link_to_test_destinaton", te.getName() );
                assertEquals( 0640, te.getMode() & UnixStat.PERM_MASK );
            }
            else
            {
                assertEquals( 0400, te.getMode() & UnixStat.PERM_MASK, "un-expected tar-entry mode for [te.name=" + te.getName() + "]" );
            }
        }
        tis.close();

    }

    private TarArchiver getPosixTarArchiver() throws Exception
    {
        TarArchiver archiver = (TarArchiver) lookup( Archiver.class, "tar" );
        archiver.setLongfile( TarLongFileMode.posix );
        return archiver;
    }

    private class TarHandler
    {

        File createTarFile()
            throws Exception
        {
            final File srcDir = new File( "src" );
            final File tarFile = new File( "target/output/src.tar" );
            TarArchiver tarArchiver = getPosixTarArchiver();
            tarArchiver.setDestFile( tarFile );
            tarArchiver.addDirectory( srcDir, null, FileUtils.getDefaultExcludes() );
            FileUtils.removePath( tarFile.getPath() );
            tarArchiver.createArchive();
            return tarFile;
        }

        File createTarfile2( File tarFile )
            throws Exception
        {
            final File tarFile2 = new File( "target/output/src2.tar" );
            TarArchiver tarArchiver2 = getPosixTarArchiver();
            tarArchiver2.setDestFile( tarFile2 );
            tarArchiver2.addArchivedFileSet( tarFile, "prfx/" );
            FileUtils.removePath( tarFile2.getPath() );
            tarArchiver2.createArchive();
            return tarFile2;
        }

        TarFile newTarFile( File tarFile )
        {
            return new TarFile( tarFile );
        }

    }

    private class GZipTarHandler
        extends TarHandler
    {

        @Override
        File createTarFile()
            throws Exception
        {
            File file = super.createTarFile();
            File compressedFile = new File( file.getPath() + ".gz" );
            Compressor compressor = new GZipCompressor();
            compressor.setSource( createResource( file, file.getName() ) );
            compressor.setDestFile( compressedFile );
            compressor.compress();
            compressor.close();
            return compressedFile;
        }

        @Override
        TarFile newTarFile( File tarFile )
        {
            return new GZipTarFile( tarFile );
        }

    }

    private class BZip2TarHandler
        extends TarHandler
    {

        @Override
        File createTarFile()
            throws Exception
        {
            File file = super.createTarFile();
            File compressedFile = new File( file.getPath() + ".bz2" );
            Compressor compressor = new BZip2Compressor();
            compressor.setSource( createResource( file ) );
            compressor.setDestFile( compressedFile );
            compressor.compress();
            compressor.close();
            return compressedFile;
        }

        @Override
        TarFile newTarFile( File tarFile )
        {
            return new BZip2TarFile( tarFile );
        }

    }

    @Test
    public void testUncompressedResourceCollection()
        throws Exception
    {
        testCreateResourceCollection( new TarHandler() );
    }

    @Test
    public void testGzipCompressedResourceCollection()
        throws Exception
    {
        testCreateResourceCollection( new GZipTarHandler() );
    }

    @Test
    public void testGzipFIleHandleLeak()
        throws Exception
    {
        GZipTarHandler tarHandler = new GZipTarHandler();
        final File tarFile = tarHandler.createTarFile();
        final File tarFile2 = tarHandler.createTarfile2( tarFile );
        final TarFile cmp1 = tarHandler.newTarFile( tarFile );
        final TarFile cmp2 = new TarFile( tarFile2 );
        ArchiveFileComparator.forEachTarArchiveEntry( cmp1, ze1 -> assertNotNull( ze1.getName() ) );
        cmp1.close();
        cmp2.close();

    }

    @Test
    public void testBzip2CompressedResourceCollection()
        throws Exception
    {
        testCreateResourceCollection( new BZip2TarHandler() );
    }

    @Test
    public void testTarFileNotClosingInputStream()
        throws Exception
    {
        // Supposedly not closing the stream according to yjp.
        TarHandler tarHandler = new BZip2TarHandler();
        final File fileName = tarHandler.createTarFile();
        final TarFile tarFile = tarHandler.newTarFile( fileName );
        tarFile.getEntries();
        tarFile.close();
    }

    private void testCreateResourceCollection( TarHandler tarHandler )
        throws Exception
    {
        final File tarFile = tarHandler.createTarFile();
        final File tarFile2 = tarHandler.createTarfile2( tarFile );
        final TarFile cmp1 = tarHandler.newTarFile( tarFile );
        final TarFile cmp2 = new TarFile( tarFile2 );
        ArchiveFileComparator.assertTarEquals( cmp1, cmp2, "prfx/" );
        cmp1.close();
        cmp2.close();
    }

    @Test
    public void testSymlinkArchivedFileSet()
        throws Exception
    {
        final File tarFile = getTestFile( "src/test/resources/symlinks/symlinks.tar" );
        final File tarFile2 = getTestFile( "target/output/pasymlinks-archivedFileset.tar" );
        final TarArchiver tarArchiver = getPosixTarArchiver();
        tarArchiver.setDestFile( tarFile2 );
        DefaultArchivedFileSet archivedFileSet = DefaultArchivedFileSet.archivedFileSet( tarFile );
        archivedFileSet.setUsingDefaultExcludes( false );
        tarArchiver.addArchivedFileSet( archivedFileSet );
        tarArchiver.createArchive();

        final TarFile cmp1 = new TarFile( tarFile );
        final TarFile cmp2 = new TarFile( tarFile2 );
        ArchiveFileComparator.assertTarEquals( cmp1, cmp2, "" );
    }

}
