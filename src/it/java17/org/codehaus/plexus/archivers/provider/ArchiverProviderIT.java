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
import org.codehaus.plexus.archivers.config.DirectoryMode;
import org.codehaus.plexus.archivers.config.DuplicateHandling;
import org.codehaus.plexus.archivers.config.EmptyDirectoryHandling;
import org.codehaus.plexus.archivers.config.PermissionHandling;
import org.codehaus.plexus.archivers.config.PermissionPolicy;
import org.codehaus.plexus.archivers.config.FileMode;
import org.codehaus.plexus.archivers.config.FilePermissions;
import org.codehaus.plexus.archivers.config.PathPatternMatcher;
import org.codehaus.plexus.archivers.config.ResourceScan;
import org.codehaus.plexus.archivers.config.StandardExcludes;
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
        
        Archiver actual = provider.newArchiver(configurer -> configurer
        		.resource(ResourceScan.fromDirectory(directory).matcher(PathPatternMatcher
        				.includes("**/*.txt")
        				.excludes("**/ignored.txt")
        				.caseSensitive(CaseSensitivity.INSENSITIVE)
        				.standardExcludes(StandardExcludes.NONE)), 
        				emit -> { 
        					emit.prefix("content/");
        					emit.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
        				}));
        
//        Archiver actual = provider.newArchiver(configurer -> configurer.addFileSet(FileSet.fromDirectory(directory)
//                .prefix("content/")
//                .includes(List.of("**/*.txt"))
//                .excludes(List.of("**/ignored.txt"))
//                .caseSensitive(CaseSensitivity.INSENSITIVE)
//                .defaultExcludes(DefaultExcludes.IGNORE)
//                .emptyDirectories(EmptyDirectoryHandling.EXCLUDE)
//                .build()));

        assertThat(actual).isSameAs(expected);
        
        assertThat(expected.fileSet.getDirectory()).as("getDirectory").isEqualTo(directory.toFile());
        assertThat(expected.fileSet.getPrefix()).as("getPrefix").isEqualTo("content/");
        assertThat(expected.fileSet.getIncludes()).as("getIncludes").containsExactly("**/*.txt");
        assertThat(expected.fileSet.getExcludes()).as("getExcludes").containsExactly("**/ignored.txt");
        assertThat(expected.fileSet.isCaseSensitive()).as("isCaseSensitive").isFalse();
        assertThat(expected.fileSet.isUsingDefaultExcludes()).as("isUsingDefaultExcludes").isFalse();
        assertThat(expected.fileSet.isIncludingEmptyDirectories()).as("isIncludingEmptyDirectories").isFalse();
    }

    @Test
    void configuresArchivedFileSet(@TempDir Path directory) {
        Path archive = directory.resolve("dependency.jar");
        CapturingArchiver expected = new CapturingArchiver();
        ArchiverProvider provider = providerFor(expected);

        Archiver actual = provider.newArchiver(configurer -> configurer
        		.resource(ResourceScan.fromArchive(archive).matcher(PathPatternMatcher
        				.includes("**/*.class")
        				.excludes("module-info.class")
        				.caseSensitive(CaseSensitivity.PLATFORM_DEFAULT)
        				.standardExcludes(StandardExcludes.NONE)), 
        				emit -> {
        					emit.prefix("lib/");
        					emit.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
        				}));

//        provider.newArchiver(configurer -> configurer.addArchivedFileSet(ArchivedFileSet.fromArchive(archive)
//                .prefix("lib/")
//                .includes(List.of("**/*.class"))
//                .excludes(List.of("module-info.class"))
//                .caseSensitive(CaseSensitivity.PLATFORM_DEFAULT)
//                .defaultExcludes(DefaultExcludes.IGNORE)
//                .emptyDirectories(EmptyDirectoryHandling.EXCLUDE)
//                .build()));

        assertThat(expected.archivedFileSet.getArchive()).as("getArchive").isEqualTo(archive.toFile());
        assertThat(expected.archivedFileSet.getPrefix()).as("getPrefix").isEqualTo("lib/");
        assertThat(expected.archivedFileSet.getIncludes()).as("getIncludes").containsExactly("**/*.class");
        assertThat(expected.archivedFileSet.getExcludes()).as("getExcludes").containsExactly("module-info.class");
        assertThat(expected.archivedFileSet.isCaseSensitive()).as("isCaseSensitive")
                .isEqualTo(CaseSensitivities.resolve(CaseSensitivity.PLATFORM_DEFAULT));
        assertThat(expected.archivedFileSet.isUsingDefaultExcludes()).as("isUsingDefaultExcludes").isFalse();
        assertThat(expected.archivedFileSet.isIncludingEmptyDirectories()).as("isIncludingEmptyDirectories").isFalse();
    }

    @Test
    void exposesEveryArchiverSetterWithOneArgument(@TempDir Path directory) {
        CapturingArchiver expected = new CapturingArchiver();
        FileTime timestamp = FileTime.fromMillis(1);

        Archiver actual = providerFor(expected).newArchiver(configurer -> {
            configurer.setDestFile(directory.resolve("archive.zip"));
            configurer.setEmptyDirectoryHandling(EmptyDirectoryHandling.EXCLUDE);
            configurer.setDotFileDirectory(directory);
            configurer.setForced(ArchiveCreation.WHEN_NEEDED);
            configurer.setDuplicateBehavior(DuplicateHandling.FAIL);
            configurer.setPermissionPolicy(PermissionPolicy.umask(FilePermissions.parse("----w--w-")));
            configurer.configureReproducibleBuild(c -> { 
            	c.setDefaultLastModifiedTime(timestamp);
                c.setDefaultUid(1000);
                c.setDefaultUserName("user");
                c.setDefaultGid(1000);
                c.setDefaultGroupName("group");
                c.setFilenameComparator(Comparator.naturalOrder());
            });
        });

        assertThat(actual).isSameAs(expected);
        assertThat(expected.includeEmptyDirectories).isFalse();
        assertThat(expected.forced).isFalse();
        assertThat(expected.duplicateBehavior).isEqualTo(Archiver.DUPLICATES_FAIL);
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
        private String duplicateBehavior;

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
        public void setDuplicateBehavior(String duplicateBehavior) {
            this.duplicateBehavior = duplicateBehavior;
        }
    }
}
