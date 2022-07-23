/**
 *
 * Copyright 2018 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;

public class JarToolModularJarArchiverTest
    extends BaseJarArchiverTest
{

    private ModularJarArchiver archiver;

    /*
     * Configures the ModularJarArchiver for the test cases.
     */
    @BeforeEach
    public void setup()
        throws Exception
    {
        File jarFile = new File( "target/output/modular.jar" );
        jarFile.delete();

        archiver = getJarArchiver();
        archiver.setDestFile( jarFile );
        archiver.addDirectory( new File( "src/test/resources/java-classes" ) );
    }

    /*
     * Verify that the main class and the version are properly set for a modular JAR file.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testModularJarWithMainClassAndVersion()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        archiver.setModuleVersion( "1.0.0" );
        archiver.setModuleMainClass( "com.example.app.Main" );

        archiver.createArchive();

        // verify that the proper version and main class are set
        assertModularJarFile( archiver.getDestFile(),
            "1.0.0", "com.example.app.Main", "com.example.app", "com.example.resources" );
    }

    /*
     * Verify that when both module main class is set and the
     * manifest contains main class atribute, the manifest
     * value is overridden
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testModularJarWithManifestAndModuleMainClass()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        Manifest manifest = new Manifest();
        manifest.addConfiguredAttribute(
            new Manifest.Attribute( "Main-Class", "com.example.app.Main2" ) );
        archiver.addConfiguredManifest( manifest );
        archiver.setModuleMainClass( "com.example.app.Main" );

        archiver.createArchive();

        // Verify that the explicitly set module main class
        // overrides the manifest main
        assertModularJarFile( archiver.getDestFile(),
            null, "com.example.app.Main", "com.example.app", "com.example.resources" );
        assertManifestMainClass( archiver.getDestFile(), "com.example.app.Main" );
    }

    /**
     * Verify that when the module main class is not explicitly set,
     * the manifest main class attribute (if present) is used instead
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testModularJarWithManifestMainClassAttribute()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        Manifest manifest = new Manifest();
        manifest.addConfiguredAttribute(
            new Manifest.Attribute( "Main-Class", "com.example.app.Main2" ) );
        archiver.addConfiguredManifest( manifest );

        archiver.createArchive();

        // Verify that the the manifest main class attribute is used as module main class
        assertModularJarFile( archiver.getDestFile(),
            null, "com.example.app.Main2", "com.example.app", "com.example.resources" );
        assertManifestMainClass( archiver.getDestFile(), "com.example.app.Main2" );
    }

    /*
     * Verify that a modular JAR file is created even when no additional attributes are set.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testModularJar()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        archiver.createArchive();

        // verify that the proper version and main class are set
        assertModularJarFile( archiver.getDestFile(),
            null, null, "com.example.app", "com.example.resources" );
    }

    /*
     * Verify that exception is thrown when the modular JAR is not valid.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testInvalidModularJar()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        // Not a valid version
        archiver.setModuleVersion( "notAValidVersion" );

        assertThrows( ArchiverException.class, () -> archiver.createArchive() );
    }

    /*
     * Verify that modular JAR files could be created even
     * if the Java version does not support modules.
     */
    @Test
    @DisabledIf( "modulesAreSupported" )
    public void testModularJarPriorJava9()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        archiver.setModuleVersion( "1.0.0" );
        archiver.setModuleMainClass( "com.example.app.Main" );

        archiver.createArchive();

        // verify that the modular jar is created
        try ( ZipFile resultingArchive = new ZipFile( archiver.getDestFile() ) )
        {
            assertNotNull( resultingArchive.getEntry( "module-info.class" )  );
        }
    }

    /*
     * Verify that the compression flag is respected.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testNoCompression()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        archiver.setCompress( false );

        archiver.createArchive();

        // verify that the entries are not compressed
        try ( ZipFile resultingArchive = new ZipFile( archiver.getDestFile() ) )
        {
            Enumeration<? extends ZipEntry> entries = resultingArchive.entries();

            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = entries.nextElement();

                assertEquals( ZipEntry.STORED, entry.getMethod() );
            }
        }
    }

    /*
     * Verify that the compression set in the "plain" JAR file
     * is kept after it is updated to modular JAR file.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testCompression()
        throws Exception
    {
        archiver.addDirectory( new File( "src/test/resources/java-module-descriptor" ) );
        archiver.addFile( new File( "src/test/jars/test.jar" ), "META-INF/lib/test.jar" );
        archiver.setRecompressAddedZips( false );

        archiver.createArchive();

        // verify that the compression is kept
        try ( ZipFile resultingArchive = new ZipFile( archiver.getDestFile() ) )
        {
            Enumeration<? extends ZipEntry> entries = resultingArchive.entries();

            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = entries.nextElement();

                int expectedMethod = entry.isDirectory() || entry.getName().endsWith( ".jar" )
                                     ? ZipEntry.STORED
                                     : ZipEntry.DEFLATED;
                assertEquals( expectedMethod, entry.getMethod() );
            }
        }
    }

    /*
     * Verify that a module descriptor in the versioned area is handled correctly.
     */
    @Test
    @EnabledIf( "modulesAreSupported" )
    public void testModularMultiReleaseJar()
        throws Exception
    {
        // Add two module-info.class, one on the root and one on the multi-release dir.
        archiver.addFile( new File( "src/test/resources/java-module-descriptor/module-info.class" ),
                          "META-INF/versions/9/module-info.class" );
        archiver.addFile( new File( "src/test/resources/java-module-descriptor/module-info.class" ),
                          "module-info.class" );

        Manifest manifest = new Manifest();
        manifest.addConfiguredAttribute( new Manifest.Attribute( "Main-Class", "com.example.app.Main2" ) );
        manifest.addConfiguredAttribute( new Manifest.Attribute( "Multi-Release", "true" ) );
        archiver.addConfiguredManifest( manifest );

        archiver.setModuleVersion( "1.0.0" );
        // This attribute overwrites the one from the manifest.
        archiver.setModuleMainClass( "com.example.app.Main" );

        SimpleDateFormat isoFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssXXX" );
        long dateTimeMillis = isoFormat.parse( "2020-02-29T23:59:59Z" ).getTime();
        FileTime lastModTime = FileTime.fromMillis( dateTimeMillis );

        archiver.configureReproducibleBuild( lastModTime );
        archiver.createArchive();

        // Round-down two seconds precision
        long roundedDown = lastModTime.toMillis() - ( lastModTime.toMillis() % 2000 );
        // Normalize to UTC
        long expectedLastModifiedTime = normalizeLastModifiedTime( roundedDown );

        // verify that the resulting modular jar has the proper version and main class set
        try ( ZipFile resultingArchive = new ZipFile( archiver.getDestFile() ) )
        {
            ZipEntry moduleDescriptorEntry = resultingArchive.getEntry( "META-INF/versions/9/module-info.class" );
            InputStream resultingModuleDescriptor = resultingArchive.getInputStream( moduleDescriptorEntry );
            assertModuleDescriptor( resultingModuleDescriptor, "1.0.0", "com.example.app.Main", "com.example.app",
                                    "com.example.resources" );

            ZipEntry rootModuleDescriptorEntry = resultingArchive.getEntry( "module-info.class" );
            InputStream rootResultingModuleDescriptor = resultingArchive.getInputStream( rootModuleDescriptorEntry );
            assertModuleDescriptor( rootResultingModuleDescriptor, "1.0.0", "com.example.app.Main", "com.example.app",
                                    "com.example.resources" );

            // verify every entry has the correct last modified time
            Enumeration<? extends ZipEntry> entries = resultingArchive.entries();
            while ( entries.hasMoreElements() )
            {
                ZipEntry element = entries.nextElement();
                assertEquals( expectedLastModifiedTime, element.getTime(),
                              "Last Modified Time does not match with expected" );
                FileTime expectedFileTime = FileTime.fromMillis( expectedLastModifiedTime );
                assertEquals( expectedFileTime, element.getLastModifiedTime(),
                              "Last Modified Time does not match with expected" );
            }
        }
    }

    @Override
    protected JarToolModularJarArchiver getJarArchiver()
    {
        return new JarToolModularJarArchiver();
    }

    private void assertModularJarFile( File jarFile ,
                                       String expectedVersion, String expectedMainClass,
                                       String... expectedPackages )
        throws Exception
    {
        try ( ZipFile resultingArchive = new ZipFile( jarFile ) )
        {
            ZipEntry moduleDescriptorEntry = resultingArchive.getEntry( "module-info.class" );
            InputStream resultingModuleDescriptor = resultingArchive.getInputStream( moduleDescriptorEntry );

            assertModuleDescriptor( resultingModuleDescriptor,
                expectedVersion, expectedMainClass, expectedPackages );
        }
    }

    private void assertModuleDescriptor( InputStream moduleDescriptorInputStream,
                                         String expectedVersion, String expectedMainClass,
                                         String... expectedPackages )
        throws Exception
    {
        // ModuleDescriptor methods are available from Java 9 so let's get by reflection
        Class<?> moduleDescriptorClass = Class.forName( "java.lang.module.ModuleDescriptor" );
        Class<?> optionalClass = Class.forName( "java.util.Optional" );
        Method readMethod = moduleDescriptorClass.getMethod( "read", InputStream.class );
        Method mainClassMethod = moduleDescriptorClass.getMethod( "mainClass" );
        Method rawVersionMethod = moduleDescriptorClass.getMethod( "rawVersion" );
        Method packagesMethod = moduleDescriptorClass.getMethod( "packages" );
        Method isPresentMethod = optionalClass.getMethod( "isPresent" );
        Method getMethod = optionalClass.getMethod( "get" );

        // Read the module from the input stream
        Object moduleDescriptor = readMethod.invoke( null, moduleDescriptorInputStream );

        // Get the module main class
        Object mainClassOptional = mainClassMethod.invoke( moduleDescriptor );
        String actualMainClass = null;
        if ( (boolean) isPresentMethod.invoke( mainClassOptional ) )
        {
            actualMainClass = (String) getMethod.invoke( mainClassOptional );
        }

        // Get the module version
        Object versionOptional = rawVersionMethod.invoke( moduleDescriptor );
        String actualVersion = null;
        if ( (boolean) isPresentMethod.invoke( versionOptional ) )
        {
            actualVersion = (String) getMethod.invoke( versionOptional );
        }

        // Get the module packages
        Set<String> actualPackagesSet = (Set<String>) packagesMethod.invoke( moduleDescriptor );
        Set<String> expectedPackagesSet = new HashSet<>( Arrays.asList( expectedPackages ) );

        assertEquals( expectedMainClass, actualMainClass );
        assertEquals( expectedVersion, actualVersion );
        assertEquals( expectedPackagesSet, actualPackagesSet );
    }

    private void assertManifestMainClass( File jarFile, String expectedMainClass )
        throws Exception
    {
        try ( ZipFile resultingArchive = new ZipFile( jarFile ) )
        {
            ZipEntry manifestEntry = resultingArchive.getEntry( "META-INF/MANIFEST.MF" );
            InputStream manifestInputStream = resultingArchive.getInputStream( manifestEntry );

            // Get the manifest main class attribute
            Manifest manifest = new Manifest( manifestInputStream );
            String actualManifestMainClass = manifest.getMainAttributes().getValue( "Main-Class" );

            assertEquals( expectedMainClass, actualManifestMainClass );
        }

    }

    /*
     * Returns true if the current version of Java does support modules.
     */
    private boolean modulesAreSupported()
    {
        try
        {
            Class.forName( "java.lang.module.ModuleDescriptor" );
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }

        return true;
    }

}
