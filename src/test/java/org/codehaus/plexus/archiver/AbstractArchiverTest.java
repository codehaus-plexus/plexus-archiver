package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractArchiverTest
{

    private AbstractArchiver archiver;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.archiver = new AbstractArchiver()
        {

            @Override
            protected String getArchiveType()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void execute() throws ArchiverException, IOException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void close() throws IOException
            {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Test
    public void testModesAndOverridesAreUnsetByDefault()
    {
        assertEquals( -1, archiver.getDefaultFileMode() );
        assertEquals( -1, archiver.getOverrideFileMode() );

        assertEquals( Archiver.DEFAULT_DIR_MODE, archiver.getDefaultDirectoryMode() );
        assertEquals( -1, archiver.getOverrideDirectoryMode() );
    }

    @Test
    public void testWhenUnsetModeUsesDefault()
    {
        assertEquals( Archiver.DEFAULT_FILE_MODE, archiver.getFileMode() );
        assertEquals( Archiver.DEFAULT_DIR_MODE, archiver.getDirectoryMode() );
    }

    @Test
    public void testSetModeIsUsedWithFlagsForType()
    {
        archiver.setFileMode( 0400 );
        assertEquals( 0100400, archiver.getFileMode() );

        archiver.setDirectoryMode( 0600 );
        assertEquals( 040600, archiver.getDirectoryMode() );
    }

    @Test
    public void testSetDefaultIncludesFlagsForType()
    {
        archiver.setDefaultFileMode( 0400 );
        assertEquals( 0100400, archiver.getDefaultFileMode() );

        archiver.setDefaultDirectoryMode( 0600 );
        assertEquals( 040600, archiver.getDefaultDirectoryMode() );
    }

    @Test
    public void testDefaultIsUsedWhenModeIsUnset()
    {
        archiver.setDefaultFileMode( 0400 );
        assertEquals( 0100400, archiver.getFileMode() );

        archiver.setDefaultDirectoryMode( 0600 );
        assertEquals( 040600, archiver.getDirectoryMode() );
    }

    @Test
    public void testOverridesCanBeReset()
    {
        archiver.setFileMode( 0400 );
        archiver.setFileMode( -1 );
        assertEquals( -1, archiver.getOverrideFileMode() );

        archiver.setDirectoryMode( 0600 );
        archiver.setDirectoryMode( -1 );
        assertEquals( -1, archiver.getOverrideDirectoryMode() );
    }

    @Test
    public void testSetDestFileInTheWorkingDir() {
        archiver.setDestFile( new File( "archive" ) );
    }

}
