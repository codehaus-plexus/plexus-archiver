package org.codehaus.plexus.archiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * An {@link ArchiveFinalizer} that process dot files with archiver directives
 * contained within. This basically means you can communicate archive creation
 * instructions between processes using dot files.
 *
 * @author Jason van Zyl
 */
public class DotDirectiveArchiveFinalizer
    extends AbstractArchiveFinalizer
{

    private static final String DEFAULT_DOT_FILE_PREFIX = ".plxarc";

    private final File dotFileDirectory;

    private final String dotFilePrefix;

    public DotDirectiveArchiveFinalizer( File dotFileDirectory )
    {
        this( dotFileDirectory, DEFAULT_DOT_FILE_PREFIX );
    }

    public DotDirectiveArchiveFinalizer( File dotFileDirectory, String dotFilePrefix )
    {
        this.dotFileDirectory = dotFileDirectory;

        this.dotFilePrefix = dotFilePrefix;
    }

    @Override
    public void finalizeArchiveCreation( Archiver archiver )
        throws ArchiverException
    {
        try
        {
            List<File> dotFiles = FileUtils.getFiles( dotFileDirectory, dotFilePrefix + "*", null );

            for ( File dotFile : dotFiles )
            {
                try ( BufferedReader in = Files.newBufferedReader( dotFile.toPath(), StandardCharsets.UTF_8 ) )
                {

                    for ( String line = in.readLine(); line != null; line = in.readLine() )
                    {
                        String[] s = StringUtils.split( line, ":" );

                        if ( s.length == 1 )
                        {
                            File directory = new File( dotFileDirectory, s[0] );

                            System.out.println( "adding directory = " + directory );

                            archiver.addFileSet( new DefaultFileSet( directory ) );
                        }
                        else
                        {
                            File directory = new File( dotFileDirectory, s[0] );

                            System.out.println( "adding directory = " + directory + " to: " + s[1] );

                            if ( s[1].endsWith( "/" ) )
                            {

                                archiver.addFileSet( new DefaultFileSet( directory ).prefixed( s[1] ) );
                            }
                            else
                            {
                                archiver.addFileSet( new DefaultFileSet( directory ).prefixed( s[1] + "/" ) );
                            }
                        }
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new ArchiverException( "Error processing dot files.", e );
        }
    }

    @Override
    public List getVirtualFiles()
    {
        return Collections.EMPTY_LIST;
    }

}
