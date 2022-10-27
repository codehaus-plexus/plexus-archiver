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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link AbstractUnArchiver}
 *
 * @author <a href="mailto:karg@quipsy.de">Markus KARG</a>
 */
public class AbstractUnArchiverTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AbstractUnArchiver abstractUnArchiver;
    private CapturingLog log = new CapturingLog( 0 /*debug*/, "AbstractUnArchiver" );

    @Before
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
        this.abstractUnArchiver.enableLogging( log );
    }

    @After
    public void tearDown()
    {
        this.abstractUnArchiver = null;
    }

    @Test
    public void shouldThrowExceptionBecauseRewrittenPathIsOutOfDirectory()
        throws ArchiverException, IOException
    {
        // given
        this.thrown.expectMessage( "Entry is outside of the target directory (../PREFIX/ENTRYNAME.SUFFIX)" );
        final File targetFolder = temporaryFolder.newFolder();
        final FileMapper[] fileMappers = new FileMapper[] { new FileMapper()
        {
            @Override
            public String getMappedFileName( String pName )
            {
                return "../PREFIX/" + pName;
            }
        }, new FileMapper()
        {
            @Override
            public String getMappedFileName( String pName )
            {
                return pName + ".SUFFIX";
            }
        } };

        // when
        this.abstractUnArchiver.extractFile( null, targetFolder, null, "ENTRYNAME", null, false, null, null,
                                             fileMappers );

        // then
        // ArchiverException is thrown providing the rewritten path
    }

    @Test
    public void shouldExtractWhenFileOnDiskDoesNotExist() throws IOException
    {
        // given
        File file = new File( temporaryFolder.getRoot(), "whatever.txt" ); // does not create the file!
        String entryname = file.getName();
        Date entryDate = new Date();

        // when & then
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is ( true ) );
    }

    @Test
    public void shouldNotExtractWhenFileOnDiskIsNewerThanEntryInArchive() throws IOException
    {
        // given
        File file = temporaryFolder.newFile();
        file.setLastModified( System.currentTimeMillis() );
        String entryname = file.getName();
        Date entryDate = new Date( 0 );

        // when & then
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is ( false ) );
    }

    @Test
    public void shouldNotExtractWhenFileOnDiskIsNewerThanEntryInArchive_andWarnAboutDifferentCasing() throws IOException
    {
        // given
        File file = temporaryFolder.newFile();
        file.setLastModified( System.currentTimeMillis() );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( 0 );

        // when & then
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is ( false ) );
        assertThat( this.log.getWarns(), hasItem( new LogMessageMatcher( "names differ only by case" ) ) );
    }

    @Test
    public void shouldExtractWhenEntryInArchiveIsNewerThanFileOnDisk() throws IOException
    {
        // given
        File file = temporaryFolder.newFile();
        file.setLastModified( 0 );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( System.currentTimeMillis() );

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is( true ) );

        // when & then
        this.abstractUnArchiver.setOverwrite( false );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is( false ) );
    }

    @Test
    public void shouldExtractWhenEntryInArchiveIsNewerThanFileOnDiskAndWarnAboutDifferentCasing() throws IOException
    {
        // given
        File file = temporaryFolder.newFile();
        file.setLastModified( 0 );
        String entryname = file.getName().toUpperCase();
        Date entryDate = new Date( System.currentTimeMillis() );

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is( true ) );
        this.abstractUnArchiver.setOverwrite( false );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is( false ) );
        assertThat( this.log.getWarns(), hasItem( new LogMessageMatcher( "names differ only by case" ) ) );
    }

    @Test
    public void shouldNotWarnAboutDifferentCasingForDirectoryEntries() throws IOException
    {
        // given
        File file = temporaryFolder.newFolder();
        file.setLastModified( 0 );
        String entryname = file.getName() + '/'; // archive entries for directories end with a '/'
        Date entryDate = new Date();

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is( true ) );
        assertThat( this.log.getWarns(), not( hasItem( new LogMessageMatcher( "names differ only by case" ) ) ) );
    }


    @Test
    public void shouldExtractWhenCasingDifferOnlyInEntryNamePath() throws IOException
    {
        // given
        File file = new File( temporaryFolder.getRoot() + File.separator + "directory", "whatever.txt" ); // does not create the file!
        file.mkdirs();
        file.createNewFile();
        String entryname = "directory/whatever.txt";
        Date entryDate = new Date(System.currentTimeMillis()+5000);

        // when & then
        this.abstractUnArchiver.setOverwrite( true );
        assertThat( this.abstractUnArchiver.shouldExtractEntry( temporaryFolder.getRoot(), file, entryname, entryDate ), is ( true ) );
    }

    static class LogMessageMatcher extends BaseMatcher<CapturingLog.Message> {
        private final StringContains delegateMatcher;

        LogMessageMatcher( String needle )
        {
            this.delegateMatcher = new StringContains( needle );
        }

        @Override
        public void describeTo( Description description )
        {
            description.appendText( "a log message with " );
            delegateMatcher.describeTo( description );
        }

        @Override
        public boolean matches( Object item )
        {
            if ( item instanceof CapturingLog.Message )
            {
                CapturingLog.Message message = (CapturingLog.Message) item;
                String haystack = message.message;
                return delegateMatcher.matches( haystack );
            }
            return false;
        }
    }
}
