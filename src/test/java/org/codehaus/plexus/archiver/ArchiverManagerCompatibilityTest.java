/*
 * Copyright MojoHaus and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.plexus.archiver;

import javax.inject.Provider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.DefaultArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the 4.x {@link DefaultArchiverManager} usable the way it was up to 4.15.0: from another
 * package, subclassed with overridden lookups, and through reflection.
 */
class ArchiverManagerCompatibilityTest {

    private static final Archiver ARCHIVER = new ZipArchiver();

    private static final UnArchiver UNARCHIVER = new ZipUnArchiver();

    private static final class OverridingArchiverManager extends DefaultArchiverManager {

        OverridingArchiverManager() {
            super(
                    Collections.<String, Provider<Archiver>>emptyMap(),
                    Collections.<String, Provider<UnArchiver>>emptyMap(),
                    Collections.<String, Provider<PlexusIoResourceCollection>>emptyMap());
        }

        @Override
        public Archiver getArchiver(String archiverName) {
            return ARCHIVER;
        }

        @Override
        public Archiver getArchiver(File file) {
            return ARCHIVER;
        }

        @Override
        public UnArchiver getUnArchiver(String unArchiverName) {
            return UNARCHIVER;
        }

        @Override
        public UnArchiver getUnArchiver(File file) {
            return UNARCHIVER;
        }
    }

    @Test
    void subclassCanOverrideLookups() throws NoSuchArchiverException {
        ArchiverManager manager = new OverridingArchiverManager();

        assertThat(manager.getArchiver("unknown")).isSameAs(ARCHIVER);
        assertThat(manager.getArchiver(new File("a.unknown"))).isSameAs(ARCHIVER);
        assertThat(manager.getUnArchiver("unknown")).isSameAs(UNARCHIVER);
        assertThat(manager.getUnArchiver(new File("a.unknown"))).isSameAs(UNARCHIVER);
    }

    @Test
    void publicMethodsAreReflectivelyCallableThroughDefaultArchiverManager() throws Exception {
        Map<String, Provider<Archiver>> archivers = Collections.singletonMap("zip", () -> ARCHIVER);
        DefaultArchiverManager manager = new DefaultArchiverManager(
                archivers,
                Collections.<String, Provider<UnArchiver>>emptyMap(),
                Collections.<String, Provider<PlexusIoResourceCollection>>emptyMap());

        Method getArchiver = DefaultArchiverManager.class.getMethod("getArchiver", String.class);
        assertThat(getArchiver.getDeclaringClass()).isEqualTo(DefaultArchiverManager.class);
        assertThat(getArchiver.invoke(manager, "zip")).isSameAs(ARCHIVER);

        for (Method method : ArchiverManager.class.getMethods()) {
            assertThat(DefaultArchiverManager.class
                            .getMethod(method.getName(), method.getParameterTypes())
                            .getDeclaringClass())
                    .as(method.toString())
                    .isEqualTo(DefaultArchiverManager.class);
        }
    }
}
