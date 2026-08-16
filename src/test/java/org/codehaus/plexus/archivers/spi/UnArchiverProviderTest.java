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

package org.codehaus.plexus.archivers.spi;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiverProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class UnArchiverProviderTest {

    @Test
    void exposesOnlyConfiguredCreation() {
        assertThat(UnArchiverProvider.class.getPermittedSubclasses()).containsExactly(AbstractUnArchiverProvider.class);
        assertThat(Arrays.stream(UnArchiverProvider.class.getMethods())
                        .filter(method -> method.getName().equals("newUnarchiver"))
                        .map(method -> method.getParameterCount()))
                .containsExactly(1);
        assertThat(Arrays.stream(AbstractUnArchiverProvider.class.getMethods()).map(method -> method.getName()))
                .doesNotContain("create");
        assertThat(Arrays.stream(UnArchiverConfigurer.class.getMethods())
                        .filter(method -> Modifier.isStatic(method.getModifiers())))
                .isEmpty();
    }

    @Test
    void configuresUnarchiverBeforeReturningIt(@TempDir Path directory) {
        Path source = directory.resolve("source.zip");
        Path destination = directory.resolve("output");

        UnArchiver unarchiver = new ZipUnArchiverProvider().newUnarchiver(configurer -> {
            configurer.setSource(source);
            configurer.setDestinationDirectory(destination);
            configurer.setExistingFileHandling(ExistingFileHandling.KEEP_NEWER);
            configurer.setFileMappers(List.of());
            configurer.setFileSelectors(List.of());
            configurer.setPermissionHandling(PermissionHandling.IGNORE);
        });

        assertThat(unarchiver.getSourceFile()).isEqualTo(source.toFile());
        assertThat(unarchiver.getDestDirectory()).isEqualTo(destination.toFile());
        assertThat(unarchiver.isOverwrite()).isFalse();
        assertThat(unarchiver.isIgnorePermissions()).isTrue();
        assertThat(unarchiver.getFileMappers()).isEmpty();
        assertThat(unarchiver.getFileSelectors()).isEmpty();
    }
}
