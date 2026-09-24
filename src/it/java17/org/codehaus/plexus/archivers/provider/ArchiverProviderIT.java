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

package org.codehaus.plexus.archivers.provider;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.archivers.config.ArchiveCreation;
import org.codehaus.plexus.archivers.config.ArchiverConfigurer;
import org.codehaus.plexus.archivers.config.CaseSensitivities;
import org.codehaus.plexus.archivers.config.CaseSensitivity;
import org.codehaus.plexus.archivers.config.DefaultExcludes;
import org.codehaus.plexus.archivers.config.DuplicateHandling;
import org.codehaus.plexus.archivers.config.EmptyDirectoryHandling;
import org.codehaus.plexus.archivers.config.PermissionHandling;
import org.codehaus.plexus.archivers.config.FilePermissions;
import org.codehaus.plexus.archivers.provider.AbstractArchiverProvider;
import org.codehaus.plexus.archivers.provider.ArchiverProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiverProviderIT {

    @Test
    void doesNotExposeUnconfiguredArchiverCreation() {
        assertThat(ArchiverProvider.class.getPermittedSubclasses()).containsExactly(AbstractArchiverProvider.class);
        assertThat(Arrays.stream(ArchiverProvider.class.getMethods())
                        .filter(method -> method.getName().equals("newArchiver"))
                        .map(method -> method.getParameterCount()))
                .containsExactly(1);
        assertThat(Arrays.stream(AbstractArchiverProvider.class.getMethods()).map(method -> method.getName()))
                .doesNotContain("create");
        assertThat(Arrays.stream(ArchiverConfigurer.class.getMethods())
                        .filter(method -> Modifier.isStatic(method.getModifiers())))
        				.satisfiesExactly(m -> { 
        					assertThat(m.getName()).isEqualTo("of");
        					assertThat(m.getParameterCount()).isEqualTo(1);
        					assertThat(m.getParameterTypes()[0]).isEqualTo(Archiver.class);
        					assertThat(m.getReturnType()).isEqualTo(ArchiverConfigurer.class);
        				});
    }

    @Test
    void configuresNewArchiverWithoutExposingFileSetImplementations(@TempDir Path directory) {
        CapturingArchiver expected = new CapturingArchiver();
        ArchiverProvider provider = providerFor(expected);

        Archiver actual = provider.newArchiver(configurer -> configurer.addFileSet(FileSet.fromDirectory(directory)
                .prefix("content/")
                .includes(List.of("**/*.txt"))
                .excludes(List.of("**/ignored.txt"))
                .caseSensitive(CaseSensitivity.INSENSITIVE)
                .defaultExcludes(DefaultExcludes.IGNORE)
                .emptyDirectories(EmptyDirectoryHandling.EXCLUDE)
                .build()));

        assertThat(actual).isSameAs(expected);
        assertThat(expected.fileSet.getDirectory()).isEqualTo(directory.toFile());
        assertThat(expected.fileSet.getPrefix()).isEqualTo("content/");
        assertThat(expected.fileSet.getIncludes()).containsExactly("**/*.txt");
        assertThat(expected.fileSet.getExcludes()).containsExactly("**/ignored.txt");
        assertThat(expected.fileSet.isCaseSensitive()).isFalse();
        assertThat(expected.fileSet.isUsingDefaultExcludes()).isFalse();
        assertThat(expected.fileSet.isIncludingEmptyDirectories()).isFalse();
    }

    @Test
    void configuresArchivedFileSet(@TempDir Path directory) {
        Path archive = directory.resolve("dependency.jar");
        CapturingArchiver expected = new CapturingArchiver();
        ArchiverProvider provider = providerFor(expected);

        provider.newArchiver(configurer -> configurer.addArchivedFileSet(ArchivedFileSet.fromArchive(archive)
                .prefix("lib/")
                .includes(List.of("**/*.class"))
                .excludes(List.of("module-info.class"))
                .caseSensitive(CaseSensitivity.PLATFORM_DEFAULT)
                .defaultExcludes(DefaultExcludes.IGNORE)
                .emptyDirectories(EmptyDirectoryHandling.EXCLUDE)
                .build()));

        assertThat(expected.archivedFileSet.getArchive()).isEqualTo(archive.toFile());
        assertThat(expected.archivedFileSet.getPrefix()).isEqualTo("lib/");
        assertThat(expected.archivedFileSet.getIncludes()).containsExactly("**/*.class");
        assertThat(expected.archivedFileSet.getExcludes()).containsExactly("module-info.class");
        assertThat(expected.archivedFileSet.isCaseSensitive())
                .isEqualTo(CaseSensitivities.resolve(CaseSensitivity.PLATFORM_DEFAULT));
        assertThat(expected.archivedFileSet.isUsingDefaultExcludes()).isFalse();
        assertThat(expected.archivedFileSet.isIncludingEmptyDirectories()).isFalse();
    }

    @Test
    void exposesEveryArchiverSetterWithOneArgument(@TempDir Path directory) {
        CapturingArchiver expected = new CapturingArchiver();
        FileTime timestamp = FileTime.fromMillis(1);

        Archiver actual = providerFor(expected).newArchiver(configurer -> {
            configurer.setDestFile(directory.resolve("archive.zip"));
            configurer.setFileMode(FilePermissions.parse("rw-r--r--"));
            configurer.setDefaultFileMode(FilePermissions.parse("rw-r-----"));
            configurer.setDirectoryMode(FilePermissions.parse("rwxr-xr-x"));
            configurer.setDefaultDirectoryMode(FilePermissions.parse("rwxr-x---"));
            configurer.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
            configurer.setDotFileDirectory(directory);
            configurer.setForced(ArchiveCreation.WHEN_NEEDED);
            configurer.setDuplicateBehavior(DuplicateHandling.FAIL);
            configurer.setIgnorePermissions(PermissionHandling.IGNORE);
            configurer.setLastModifiedTime(timestamp);
            configurer.setFilenameComparator(Comparator.naturalOrder());
            configurer.setOverrideUid(1000);
            configurer.setOverrideUserName("user");
            configurer.setOverrideGid(1000);
            configurer.setOverrideGroupName("group");
            configurer.setUmask(FilePermissions.parse("----w--w-"));
            configurer.configureReproducibleBuild(c -> c.setDefaultLastModifiedTime(timestamp));
        });

        assertThat(actual).isSameAs(expected);
        assertThat(expected.includeEmptyDirectories).isFalse();
        assertThat(expected.forced).isFalse();
        assertThat(expected.ignorePermissions).isTrue();
        assertThat(expected.fileMode).isEqualTo(0644);
        assertThat(expected.defaultFileMode).isEqualTo(0640);
        assertThat(expected.directoryMode).isEqualTo(0755);
        assertThat(expected.defaultDirectoryMode).isEqualTo(0750);
        assertThat(expected.duplicateBehavior).isEqualTo(Archiver.DUPLICATES_FAIL);
        assertThat(expected.umask).isEqualTo(0022);
    }

    private static ArchiverProvider providerFor(Archiver archiver) {
        return new AbstractArchiverProvider() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            protected Archiver createArchiver() {
                return archiver;
            }
        };
    }

    private static final class CapturingArchiver extends NoOpArchiver {
        private FileSet fileSet;
        private ArchivedFileSet archivedFileSet;
        private boolean includeEmptyDirectories;
        private boolean forced;
        private boolean ignorePermissions;
        private int fileMode;
        private int defaultFileMode;
        private int directoryMode;
        private int defaultDirectoryMode;
        private String duplicateBehavior;
        private int umask;

        @Override
        public void addFileSet(FileSet fileSet) {
            this.fileSet = fileSet;
        }

        @Override
        public void addArchivedFileSet(ArchivedFileSet archivedFileSet) {
            this.archivedFileSet = archivedFileSet;
        }

        @Override
        public void setIncludeEmptyDirs(boolean includeEmptyDirectories) {
            this.includeEmptyDirectories = includeEmptyDirectories;
        }

        @Override
        public void setForced(boolean forced) {
            this.forced = forced;
        }

        @Override
        public void setIgnorePermissions(boolean ignorePermissions) {
            this.ignorePermissions = ignorePermissions;
        }

        @Override
        public void setFileMode(int mode) {
            this.fileMode = mode;
        }

        @Override
        public void setDefaultFileMode(int mode) {
            this.defaultFileMode = mode;
        }

        @Override
        public void setDirectoryMode(int mode) {
            this.directoryMode = mode;
        }

        @Override
        public void setDefaultDirectoryMode(int mode) {
            this.defaultDirectoryMode = mode;
        }

        @Override
        public void setDuplicateBehavior(String duplicateBehavior) {
            this.duplicateBehavior = duplicateBehavior;
        }

        @Override
        public void setUmask(int umask) {
            this.umask = umask;
        }
    }
}
