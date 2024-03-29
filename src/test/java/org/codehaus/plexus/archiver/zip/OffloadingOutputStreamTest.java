/*
 * Copyright The Plexus developers.
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

package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OffloadingOutputStreamTest {

    @Test
    void temporaryFileShouldBeCreated() throws IOException {
        File streamFile = null;
        try (OffloadingOutputStream stream = new OffloadingOutputStream(100, "test", "test")) {
            stream.write(new byte[256]);
            stream.close();
            streamFile = stream.getFile();
            assertThat(streamFile).isFile().hasSize(256);
        } finally {
            if (streamFile != null) {
                Files.delete(streamFile.toPath());
            }
        }
    }
}
