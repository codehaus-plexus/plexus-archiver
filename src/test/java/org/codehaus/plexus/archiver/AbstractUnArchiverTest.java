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
package org.codehaus.plexus.archiver;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link AbstractUnArchiver}
 *
 * @author <a href="mailto:karg@quipsy.de">Markus KARG</a>
 */
public class AbstractUnArchiverTest
{
    private AbstractUnArchiver abstractUnArchiver;

    @BeforeEach
    public void setUp()
    {
        this.abstractUnArchiver = new AbstractUnArchiver()
        {
            @Override
            protected void execute( final String path, final File outputDirectory )
                throws ArchiverException
            {
                // unused
            }

            @Override
            protected void execute()
                throws ArchiverException
            {
                // unused
            }
        };
    }

    @AfterEach
    public void tearDown()
    {
        this.abstractUnArchiver = null;
    }

    @Test
    public void shouldThrowExceptionBecauseRewrittenPathIsOutOfDirectory( @TempDir File targetFolder )
        throws ArchiverException
    {
        // given
        final FileMapper[] fileMappers = new FileMapper[] { pName -> "../PREFIX/" + pName, pName -> pName + ".SUFFIX"};

        // when
        Exception exception = assertThrows( ArchiverException.class, () ->
            abstractUnArchiver.extractFile( null, targetFolder, null, "ENTRYNAME", null, false, null, null, fileMappers ) );
        // then
        // ArchiverException is thrown providing the rewritten path
        assertEquals( "Entry is outside of the target directory (../PREFIX/ENTRYNAME.SUFFIX)", exception.getMessage() );
    }

    @Test
    public void shouldExtractWhenFileOnDiskDoesNotExist(  @TempDir File temporaryFolder ) throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" ); // does not create the file!
        String entryname = file.getName();
        Date entryDate = new Date();

        // when & then
        // always extract the file if it does not exist
        assertTrue( abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        abstractUnArchiver.setOverwrite( false );
        assertTrue( abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
    }

    @Test
    public void shouldExtractWhenFileOnDiskIsNewerThanEntryInArchive( @TempDir File temporaryFolder ) throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" );
        file.createNewFile();
        file.setLastModified( System.currentTimeMillis() );
        String entryname = file.getName();
        Date entryDate = new Date( 0 );

        // when & then
        // if the file is newer than archive entry, extract only if overwrite is true (default)
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        abstractUnArchiver.setOverwrite( false );
        assertFalse( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
    }

    @Test
    public void shouldExtractWhenFileOnDiskIsNewerThanEntryInArchive_andWarnAboutDifferentCasing( @TempDir File temporaryFolder )
        throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" );
        file.createNewFile();
        file.setLastModified( System.currentTimeMillis() );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( 0 );

        // when & then
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        assertTrue( this.abstractUnArchiver.casingMessageEmitted.get() > 0 );
    }

    @Test
    public void shouldExtractWhenEntryInArchiveIsNewerThanFileOnDisk( @TempDir File temporaryFolder ) throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" );
        file.createNewFile();
        file.setLastModified( 0 );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( System.currentTimeMillis() );

        // when & then
        // always extract the file if the archive entry is newer than the file on disk
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        this.abstractUnArchiver.setOverwrite( false );
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
    }

    @Test
    public void shouldExtractWhenEntryInArchiveIsNewerThanFileOnDiskAndWarnAboutDifferentCasing( @TempDir File temporaryFolder )
        throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" );
        file.createNewFile();
        file.setLastModified( 0 );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( System.currentTimeMillis() );

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        this.abstractUnArchiver.setOverwrite( false );
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        assertTrue( this.abstractUnArchiver.casingMessageEmitted.get() > 0 );
    }

    @Test
    public void shouldNotWarnAboutDifferentCasingForDirectoryEntries( @TempDir File temporaryFolder )
        throws IOException
    {
        // given
        File file = new File( temporaryFolder, "whatever.txt" );
        file.createNewFile();
        file.setLastModified( 0 );
        String entryname = file.getName() + '/'; // archive entries for directories end with a '/'
        Date entryDate = new Date();

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertTrue( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryname, entryDate ) );
        assertEquals( 0, this.abstractUnArchiver.casingMessageEmitted.get() );
    }

    @Test
    public void shouldExtractWhenCasingDifferOnlyInEntryNamePath( @TempDir File temporaryFolder)
            throws IOException
    {
        // given
        String entryName = "directory/whatever.txt";
        File file = new File( temporaryFolder, entryName ); // does not create the file!
        file.mkdirs();
        file.createNewFile();
        Date entryDate = new Date(System.currentTimeMillis() + 5000);

        // when & then
        abstractUnArchiver.setOverwrite( true );
        assertTrue( abstractUnArchiver.shouldExtractEntry( temporaryFolder, file, entryName, entryDate ) );
        assertEquals(0, abstractUnArchiver.casingMessageEmitted.get());
    }
}
