package org.codehaus.plexus.archiver.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import org.codehaus.plexus.archiver.ArchiverException;
import junit.framework.TestCase;
import org.codehaus.plexus.archiver.jar.module.ModuleConfiguration;
import org.codehaus.plexus.archiver.jar.module.ModuleDescriptor;

public class JarArchiverTest
    extends TestCase
{

    public void testCreateManifestOnlyJar()
        throws IOException, ManifestException, ArchiverException
    {
        File jarFile = File.createTempFile( "JarArchiverTest.", ".jar" );
        jarFile.deleteOnExit();

        JarArchiver archiver = new JarArchiver();
        archiver.setDestFile( jarFile );

        Manifest manifest = new Manifest();
        Manifest.Attribute attribute = new Manifest.Attribute( "Main-Class", getClass().getName() );

        manifest.addConfiguredAttribute( attribute );

        archiver.addConfiguredManifest( manifest );

        archiver.createArchive();
    }

    public void testNonCompressed()
        throws IOException, ManifestException, ArchiverException
    {
        File jarFile = new File( "target/output/jarArchiveNonCompressed.jar" );

        JarArchiver archiver = new JarArchiver();
        archiver.setDestFile( jarFile );
        archiver.setCompress( false );
        archiver.addDirectory( new File( "src/test/resources/mjar179" ) );
        archiver.createArchive();
    }

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
            FileOutputStream out = new FileOutputStream( f );
            byte[] data = new byte[ 512 ]; // 512bytes per file
            rand.nextBytes( data );
            out.write( data );
            out.flush();
            out.close();
        }

        File jarFile = new File( "target/output/veryLargeJar.jar" );

        JarArchiver archiver = new JarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( tmpDir );
        archiver.createArchive();
    }

    public void testModularJar()
        throws IOException
    {
        File jarFile = new File( "target/output/modular.jar" );
        jarFile.delete();

        // verify that the original module declaration does not have main class and version set
        ModuleDescriptor originalModuleDescriptor = ModuleDescriptor.read(
            new File( "src/test/resources/java-module/module-info.class" ) );
        assertNull( originalModuleDescriptor.getMainClass() );
        assertNull( originalModuleDescriptor.getVersion() );

        JarArchiver archiver = new JarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( new File( "src/test/resources/java-module" ) );

        ModuleConfiguration moduleConfiguration = new ModuleConfiguration();
        moduleConfiguration.setVersion( "1.0.0" );
        moduleConfiguration.setMainClass( "com.example.app.Main" );
        archiver.setModuleConfiguration( moduleConfiguration );

        archiver.createArchive();

        // verify that the resulting modular jar has the proper version and main class set
        ModuleDescriptor resultingModuleDescriptor = ModuleDescriptor.read(
            new URL( "jar:file:target/output/modular.jar!/module-info.class" ) );
        assertEquals( "1.0.0", resultingModuleDescriptor.getVersion() );
        assertEquals( "com/example/app/Main", resultingModuleDescriptor.getMainClass() );
    }

}
