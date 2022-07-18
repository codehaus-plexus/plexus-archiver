package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.TestSupport;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jason van Zyl
 */
public class ZipUnArchiverTest
        extends TestSupport
{

    @Test
    public void testExtractingZipPreservesExecutableFlag()
        throws Exception
    {

        String s = "target/zip-unarchiver-tests";
        File testZip = new File( getBasedir(), "src/test/jars/test.zip" );
        File outputDirectory = new File( getBasedir(), s );

        FileUtils.deleteDirectory( outputDirectory );

        ZipUnArchiver zu = getZipUnArchiver( testZip );
        zu.extract( "", outputDirectory );
        File testScript = new File( outputDirectory, "test.sh" );

        final Method canExecute;
        try
        {
            canExecute = File.class.getMethod( "canExecute" );
            canExecute.invoke( testScript );
            assertTrue( (Boolean) canExecute.invoke( testScript ) );
        }
        catch ( NoSuchMethodException ignore )
        {
        }
    }

    @Test
    public void testZeroFileModeInZip()
        throws Exception
    {

        String s = "target/zip-unarchiver-filemode-tests";
        File testZip = new File( getBasedir(), "src/test/resources/zeroFileMode/foobar.zip" );
        File outputDirectory = new File( getBasedir(), s );

        FileUtils.deleteDirectory( outputDirectory );

        ZipUnArchiver zu = getZipUnArchiver( testZip );
        zu.setIgnorePermissions( false );
        zu.extract( "", outputDirectory );

        File testScript = new File( outputDirectory, "foo.txt" );

        final Method canRead;
        try
        {
            canRead = File.class.getMethod( "canRead" );
            canRead.invoke( testScript );
            assertTrue( (Boolean) canRead.invoke( testScript ) );
        }
        catch ( NoSuchMethodException ignore )
        {
        }
    }

    @Test
    public void testUnarchiveUtf8()
        throws Exception
    {
        File dest = new File( "target/output/unzip/utf8" );
        dest.mkdirs();

        final File zipFile = new File( "target/output/unzip/utf8-default.zip" );
        final ZipArchiver zipArchiver = getZipArchiver( zipFile );
        zipArchiver.addDirectory( new File( "src/test/resources/miscUtf8" ) );
        zipArchiver.createArchive();
        final ZipUnArchiver unarchiver = getZipUnArchiver( zipFile );
        unarchiver.setDestFile( dest );
        unarchiver.extract();
        assertTrue( new File( dest, "aPi\u00F1ata.txt" ).exists() );
        assertTrue( new File( dest, "an\u00FCmlaut.txt" ).exists() );
        assertTrue( new File( dest, "\u20acuro.txt" ).exists() );
    }

    @Test
    public void testUnarchiveUnicodePathExtra()
        throws Exception
    {
        File dest = new File( "target/output/unzip/unicodePathExtra" );
        dest.mkdirs();
        for ( String name : dest.list() )
        {
            new File( dest, name ).delete();
        }
        assertEquals( 0, dest.list().length );

        final ZipUnArchiver unarchiver = getZipUnArchiver( new File( "src/test/resources/unicodePathExtra/efsclear.zip" ) );
        unarchiver.setDestFile( dest );
        unarchiver.extract();
        // a Unicode Path extra field should only be used when its CRC matches the header file name
        assertEquals( new HashSet<>( Arrays.asList( "nameonly-name", "goodextra-extra", "badextra-name" ) ),
                      new HashSet<>( Arrays.asList( dest.list() ) ),
                      "should use good extra fields but not bad ones" );
    }

    @Test
    public void testUnarchiveUnicodePathExtraSelector()
        throws Exception
    {
        File dest = new File( "target/output/unzip/unicodePathExtraSelector" );
        dest.mkdirs();
        for ( String name : dest.list() )
        {
            new File( dest, name ).delete();
        }
        assertEquals( 0, dest.list().length );

        class CollectingSelector implements FileSelector
        {
            public Set<String> collection = new HashSet<>();
            @Override
            public boolean isSelected( FileInfo fileInfo ) throws IOException
            {
                collection.add( fileInfo.getName() );
                return false;
            }
        }
        CollectingSelector selector = new CollectingSelector();

        final ZipUnArchiver unarchiver = getZipUnArchiver( new File( "src/test/resources/unicodePathExtra/efsclear.zip" ) );
        unarchiver.setDestFile( dest );
        unarchiver.setFileSelectors( new FileSelector[] { selector } );
        unarchiver.extract();

        assertEquals( 0, dest.list().length, "should not extract anything" );
        // a Unicode Path extra field should only be used when its CRC matches the header file name
        assertEquals( new HashSet<>( Arrays.asList( "nameonly-name", "goodextra-extra", "badextra-name" ) ),
                      selector.collection, "should use good extra fields but not bad ones" );
    }

    private void runUnarchiver( String path, FileSelector[] selectors, boolean[] results )
        throws Exception
    {
        String s = "target/zip-unarchiver-tests";

        File testJar = new File( getBasedir(), "src/test/jars/test.jar" );

        File outputDirectory = new File( getBasedir(), s );

        ZipUnArchiver zu = getZipUnArchiver( testJar );
        zu.setFileSelectors( selectors );

        FileUtils.deleteDirectory( outputDirectory );

        zu.extract( path, outputDirectory );

        File f0 = new File( getBasedir(), s + "/resources/artifactId/test.properties" );

        assertEquals( results[0], f0.exists() );

        File f1 = new File( getBasedir(), s + "/resources/artifactId/directory/test.properties" );

        assertEquals( results[1], f1.exists() );

        File f2 = new File( getBasedir(), s + "/META-INF/MANIFEST.MF" );

        assertEquals( results[2], f2.exists() );
    }

    private ZipUnArchiver getZipUnArchiver( File testJar ) throws Exception
    {
        ZipUnArchiver zu = (ZipUnArchiver) lookup( UnArchiver.class, "zip" );
        zu.setSourceFile( testJar );
        return zu;
    }

    @Test
    public void testExtractingADirectoryFromAJarFile()
        throws Exception
    {
        runUnarchiver( "resources/artifactId", null,
                       new boolean[]
                       {
                           true, true, false
                       } );
        runUnarchiver( "", null,
                       new boolean[]
                       {
                           true, true, true
                       } );
    }

    @Test
    public void testSelectors()
        throws Exception
    {
        IncludeExcludeFileSelector fileSelector = new IncludeExcludeFileSelector();
        runUnarchiver( "", new FileSelector[]
                   {
                       fileSelector
        },
                       new boolean[]
                       {
                           true, true, true
                       } );
        fileSelector.setExcludes( new String[]
        {
            "**/test.properties"
        } );
        runUnarchiver( "", new FileSelector[]
                   {
                       fileSelector
        },
                       new boolean[]
                       {
                           false, false, true
                       } );
        fileSelector.setIncludes( new String[]
        {
            "**/test.properties"
        } );
        fileSelector.setExcludes( null );
        runUnarchiver( "", new FileSelector[]
                   {
                       fileSelector
        },
                       new boolean[]
                       {
                           true, true, false
                       } );
        fileSelector.setExcludes( new String[]
        {
            "resources/artifactId/directory/test.properties"
        } );
        runUnarchiver( "", new FileSelector[]
                   {
                       fileSelector
        },
                       new boolean[]
                       {
                           true, false, false
                       } );
    }

    @Test
    public void testExtractingZipWithEntryOutsideDestDirThrowsException()
            throws Exception
    {
        Exception ex = null;
        String s = "target/zip-unarchiver-slip-tests";
        File testZip = new File( getBasedir(), "src/test/zips/zip-slip.zip" );
        File outputDirectory = new File( getBasedir(), s );

        FileUtils.deleteDirectory( outputDirectory );

        try
        {
            ZipUnArchiver zu = getZipUnArchiver( testZip );
            zu.extract( "", outputDirectory );
        }
        catch ( Exception e )
        {
            ex = e;
        }

        assertNotNull( ex );
        assertTrue( ex.getMessage().startsWith( "Entry is outside of the target directory" ) );
    }

    @Test
    public void testZipOutputSizeException()
        throws Exception
    {
        Exception ex = null;
        String s = "target/zip-size-tests";
        File testZip = new File( getBasedir(), "src/test/jars/test.zip" );
        File outputDirectory = new File( getBasedir(), s );

        FileUtils.deleteDirectory( outputDirectory );

        try
        {
            ZipUnArchiver zu = getZipUnArchiver( testZip );
            zu.setMaxOutputSize( 10L );
            zu.extract( "", outputDirectory );
        }
        catch ( Exception e )
        {
            ex = e;
        }

        assertNotNull( ex );
        assertTrue( ex.getMessage().startsWith( "Maximum output size limit reached" ) );
    }

    @Test
    public void testZipMaxOutputSizeEqualToExtractedFileSize()
        throws Exception
    {
        long extractedFileSize = 11L;
        String s = "target/zip-size-tests";
        File testZip = new File( getBasedir(), "src/test/jars/test.zip" );
        File outputDirectory = new File( getBasedir(), s );

        FileUtils.deleteDirectory( outputDirectory );

        ZipUnArchiver zu = getZipUnArchiver( testZip );
        zu.setMaxOutputSize( extractedFileSize );
        zu.extract( "", outputDirectory );

        File extractedFile = new File( outputDirectory, "test.sh" );
        assertEquals( extractedFileSize, extractedFile.length() );
    }

    private ZipArchiver getZipArchiver()
    {
        try
        {
            return (ZipArchiver) lookup( Archiver.class, "zip" );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private ZipArchiver getZipArchiver( File destFile )
    {
        final ZipArchiver zipArchiver = getZipArchiver();
        zipArchiver.setDestFile( destFile );
        return zipArchiver;
    }

}
