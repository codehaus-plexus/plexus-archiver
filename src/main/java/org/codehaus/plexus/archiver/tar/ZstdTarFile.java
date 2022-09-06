/*
 * Copyright 2022 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.tar;

import org.codehaus.plexus.archiver.zstd.ZstdUnArchiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of {@link org.codehaus.plexus.archiver.tar.TarFile} for zst compressed files.
 */
public class ZstdTarFile extends TarFile
{

    public ZstdTarFile( File file )
    {
        super( file );
    }

    @Override
    protected InputStream getInputStream( File file ) throws IOException
    {
        return ZstdUnArchiver.getZstdInputStream( super.getInputStream( file ) );
    }

}
