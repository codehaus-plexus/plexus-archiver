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
import java.nio.file.Files;

import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link AbstractUnArchiver}
 *
 * @author <a href="mailto:karg@quipsy.de">Markus KARG</a>
 */
public class AbstractUnArchiverTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AbstractUnArchiver abstractUnArchiver;

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
        final File targetFolder = Files.createTempDirectory( null ).toFile();
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

}
