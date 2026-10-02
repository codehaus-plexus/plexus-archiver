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
package org.codehaus.plexus.archiver.jar;

import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.io.output.NullPrintStream;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.Streams;
import org.codehaus.plexus.archiver.zip.ConcurrentJarCreator;
import org.codehaus.plexus.util.IOUtil;

/**
 * A {@link ModularJarArchiver} implementation that uses
 * the {@code jar} tool provided by
 * {@code java.util.spi.ToolProvider} to create
 * modular JAR files.
 *
 * <p>
 * The basic JAR archive is created by {@link JarArchiver}
 * and the {@code jar} tool is used to upgrade it to modular JAR.
 *
 * <p>
 * If the JAR file does not contain module descriptor
 * or the JDK does not provide the {@code jar} tool
 * (for example JDK prior to Java 9), then the
 * archive created by {@link JarArchiver}
 * is left unchanged.
 */
@Named("mjar")
public class JarToolModularJarArchiver extends ModularJarArchiver {
    private static final String MODULE_DESCRIPTOR_FILE_NAME = "module-info.class";

    private static final Pattern MRJAR_VERSION_AREA = Pattern.compile("META-INF/versions/\\d+/");

    private final Object jarTool;

    private final Method jarToolRun;

    private boolean moduleDescriptorFound;

    private boolean hasJarDateOption;

    public JarToolModularJarArchiver() {
        Object tool = null;
        Method run = null;
        try {
            Class<?> toolProvider = Class.forName("java.util.spi.ToolProvider");
            Method findFirst = toolProvider.getMethod("findFirst", String.class);
            Optional<?> jarToolProvider = (Optional<?>) findFirst.invoke(null, "jar");
            if (jarToolProvider.isPresent()) {
                tool = jarToolProvider.get();
                run = toolProvider.getMethod("run", PrintStream.class, PrintStream.class, String[].class);
            }
        } catch (ClassNotFoundException e) {
            // ToolProvider is not available before Java 9.
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access the JDK jar tool", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not locate the JDK jar tool", e.getCause());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not access the JDK jar tool", e);
        }
        this.jarTool = tool;
        this.jarToolRun = run;
    }

    @Override
    protected void zipFile(
            InputStreamSupplier is,
            ConcurrentJarCreator zOut,
            String vPath,
            long lastModified,
            File fromArchive,
            int mode,
            String symlinkDestination,
            boolean addInParallel)
            throws IOException, ArchiverException {
        if (jarTool != null && isModuleDescriptor(vPath)) {
            getLogger().debug("Module descriptor found: " + vPath);

            moduleDescriptorFound = true;
        }

        super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode, symlinkDestination, addInParallel);
    }

    @Override
    protected void postCreateArchive() throws ArchiverException {
        if (!moduleDescriptorFound) {
            // no need to update the JAR archive
            return;
        }

        try {
            getLogger().debug("Using the jar tool to " + "update the archive to modular JAR.");

            if (getLastModifiedTime() != null) {
                hasJarDateOption = isJarDateOptionSupported();
                getLogger().debug("jar tool --date option is supported: " + hasJarDateOption);
            }

            int result = runJarTool(System.out, System.err, getJarToolArguments());

            if (result != 0) {
                throw new ArchiverException(
                        "Could not create modular JAR file. " + "The JDK jar tool exited with " + result);
            }

            if (!hasJarDateOption && getLastModifiedTime() != null) {
                getLogger().debug("Fix last modified time zip entries.");
                // --date option not supported, fallback to rewrite the JAR file
                // https://github.com/codehaus-plexus/plexus-archiver/issues/164
                fixLastModifiedTimeZipEntries();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ArchiverException("Exception occurred " + "while creating modular JAR file", e);
        }
    }

    /**
     * Fallback to rewrite the JAR file with the correct timestamp if the {@code --date} option is not available.
     */
    private void fixLastModifiedTimeZipEntries() throws IOException {
        long timeMillis = getLastModifiedTime().toMillis();
        Path destFile = getDestFile().toPath();
        PosixFileAttributeView view =
                Files.getFileAttributeView(destFile, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

        PosixFileAttributes posixFileAttributes = null;
        if (view != null) {
            posixFileAttributes = view.readAttributes();
        }

        FileAttribute<?>[] attributes;
        if (posixFileAttributes != null) {
            attributes = new FileAttribute<?>[1];
            attributes[0] = PosixFilePermissions.asFileAttribute(posixFileAttributes.permissions());
        } else {
            attributes = new FileAttribute<?>[0];
        }
        Path tmpZip = Files.createTempFile(destFile.getParent(), null, null, attributes);
        try {
            try (ZipFile zipFile = new ZipFile(getDestFile());
                    ZipOutputStream out = new ZipOutputStream(Streams.fileOutputStream(tmpZip))) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    // Not using setLastModifiedTime(FileTime) as it sets the extended timestamp
                    // which is not compatible with the jar tool output.
                    entry.setTime(timeMillis);
                    out.putNextEntry(entry);
                    if (!entry.isDirectory()) {
                        IOUtil.copy(zipFile.getInputStream(entry), out);
                    }
                    out.closeEntry();
                }
            }
            Files.move(tmpZip, destFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Clean up temporary file if an error occurs
            try {
                Files.delete(tmpZip);
            } catch (IOException ioe) {
                e.addSuppressed(ioe);
            }
            throw e;
        }
    }

    /**
     * Returns {@code true} if {@code path}
     * is a module descriptor.
     */
    private boolean isModuleDescriptor(String path) {
        if (path.endsWith(MODULE_DESCRIPTOR_FILE_NAME)) {
            String prefix = path.substring(0, path.lastIndexOf(MODULE_DESCRIPTOR_FILE_NAME));

            // the path is a module descriptor if it located
            // into the root of the archive or into the
            // version area of a multi-release JAR file
            return prefix.isEmpty() || MRJAR_VERSION_AREA.matcher(prefix).matches();
        } else {
            return false;
        }
    }

    /**
     * Prepares the arguments for the jar tool.
     * It takes into account the module version,
     * main class, etc.
     */
    private String[] getJarToolArguments() throws IOException {
        // We add empty temporary directory to the JAR file.
        // It may look strange at first, but to update a JAR file
        // you need to add new files[1]. If we add empty directory
        // it will be ignored (not added to the archive), but
        // the module descriptor will be updated and validated.
        //
        // [1] There are some exceptions (such as when the main class
        // is updated) but we need at least empty directory
        // to ensure it will work in all cases.
        File tempEmptyDir = Files.createTempDirectory(null).toFile();
        tempEmptyDir.deleteOnExit();

        List<String> args = new ArrayList<>();

        args.add("--update");
        args.add("--file");
        args.add(getDestFile().getAbsolutePath());

        String mainClass = getModuleMainClass() != null ? getModuleMainClass() : getManifestMainClass();

        if (mainClass != null) {
            args.add("--main-class");
            args.add(mainClass);
        }

        if (getModuleVersion() != null) {
            args.add("--module-version");
            args.add(getModuleVersion());
        }

        if (!isCompress()) {
            args.add("--no-compress");
        }

        if (hasJarDateOption) {
            // The --date option already normalize the time, so revert to the local time
            FileTime localTime = revertToLocalTime(getLastModifiedTime());
            args.add("--date");
            args.add(localTime.toString());
        }

        args.add("-C");
        args.add(tempEmptyDir.getAbsolutePath());
        args.add(".");

        return args.toArray(new String[0]);
    }

    private static FileTime revertToLocalTime(FileTime time) {
        long restoreToLocalTime = time.toMillis();
        Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
        cal.setTimeInMillis(restoreToLocalTime);
        restoreToLocalTime = restoreToLocalTime + (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
        return FileTime.fromMillis(restoreToLocalTime);
    }

    /**
     * Check support for {@code --date} option introduced since Java 17.0.3 with <a href="https://bugs.openjdk.org/browse/JDK-8277755">JDK-8277755</a>.
     *
     * @return true if the JAR tool supports the {@code --date} option
     */
    private boolean isJarDateOptionSupported() throws ArchiverException {
        // Test the output code validating the --date option.
        String[] args = {"--date", "2099-12-31T23:59:59Z", "--version"};

        PrintStream nullPrintStream = NullPrintStream.INSTANCE;

        int result = runJarTool(nullPrintStream, nullPrintStream, args);

        return result == 0;
    }

    private int runJarTool(PrintStream out, PrintStream err, String[] args) throws ArchiverException {
        try {
            return ((Integer) jarToolRun.invoke(jarTool, out, err, args)).intValue();
        } catch (IllegalAccessException e) {
            throw new ArchiverException("Could not access the JDK jar tool", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new ArchiverException("Could not invoke the JDK jar tool", cause);
        }
    }
}
