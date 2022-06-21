package org.codehaus.plexus.archiver;

import java.io.File;
import java.util.jar.JarFile;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * @author Jason van Zyl
 */
public class DotDirectiveArchiveFinalizerTest
    extends TestSupport
{

    public void testDotDirectiveArchiveFinalizer()
        throws Exception
    {
        DotDirectiveArchiveFinalizer ddaf =
            new DotDirectiveArchiveFinalizer( new File( getBasedir(), "src/test/dotfiles" ) );

        JarArchiver archiver = new JarArchiver();

        File jarFile = new File( getBasedir(), "target/dotfiles.jar" );

        archiver.setDestFile( jarFile );

        archiver.addArchiveFinalizer( ddaf );

        archiver.createArchive();

        JarFile jar = new JarFile( jarFile );

        assertNotNull( jar.getEntry( "LICENSE.txt" ) );

        assertNotNull( jar.getEntry( "NOTICE.txt" ) );

        assertNotNull( jar.getEntry( "META-INF/maven/LICENSE.txt" ) );

        assertNotNull( jar.getEntry( "META-INF/maven/NOTICE.txt" ) );
    }

    public void testDefaultDotDirectiveBehaviour()
        throws Exception
    {
        File dotFileDirectory = new File( getBasedir(), "src/test/dotfiles" );

        JarArchiver archiver = new JarArchiver();

        archiver.setDotFileDirectory( dotFileDirectory );

        File jarFile = new File( getBasedir(), "target/default-dotfiles.jar" );

        archiver.setDestFile( jarFile );

        archiver.createArchive();

        JarFile jar = new JarFile( jarFile );

        assertNotNull( jar.getEntry( "LICENSE.txt" ) );

        assertNotNull( jar.getEntry( "NOTICE.txt" ) );

        assertNotNull( jar.getEntry( "META-INF/maven/LICENSE.txt" ) );

        assertNotNull( jar.getEntry( "META-INF/maven/NOTICE.txt" ) );
    }

}
