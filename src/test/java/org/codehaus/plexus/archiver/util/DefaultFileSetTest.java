package org.codehaus.plexus.archiver.util;

import java.io.File;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.archiver.util.DefaultFileSet.fileSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class DefaultFileSetTest
{

    @Test
    public void testCreate()
    {
        final String[] includes =
        {
            "zz", "yy"
        };
        final String[] exc =
        {
            "xx1", "xx2"
        };
        final DefaultFileSet dfs = fileSet( new File( "foo" ) ).prefixed( "pfx" ).include( includes ).exclude( exc );
        assertEquals( "foo", dfs.getDirectory().getName() );
        assertEquals( "pfx", dfs.getPrefix() );
        assertEquals( "zz", dfs.getIncludes()[0] );
        assertEquals( "xx1", dfs.getExcludes()[0] );
    }

}
