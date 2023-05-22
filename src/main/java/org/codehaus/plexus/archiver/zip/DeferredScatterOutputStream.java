/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.parallel.ScatterGatherBackingStore;

public class DeferredScatterOutputStream implements ScatterGatherBackingStore {

    private final OffloadingOutputStream dfos;

    public DeferredScatterOutputStream(int threshold) {
        dfos = new OffloadingOutputStream(threshold, "scatterzipfragment", "zip");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return dfos.getInputStream();
    }

    @Override
    public void writeOut(byte[] data, int offset, int length) throws IOException {
        dfos.write(data, offset, length);
    }

    @Override
    public void closeForWriting() throws IOException {
        dfos.close();
    }

    @Override
    public void close() throws IOException {
        File file = dfos.getFile();
        if (file != null) {
            file.delete();
        }
    }
}
