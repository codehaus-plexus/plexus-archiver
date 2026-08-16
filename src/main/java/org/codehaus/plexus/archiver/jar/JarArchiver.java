/**
 *
 * Copyright 2004 The Apache Software Foundation
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ConcurrentJarCreator;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import static org.codehaus.plexus.archiver.util.Streams.bufferedOutputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileInputStream;
import static org.codehaus.plexus.archiver.util.Streams.fileOutputStream;

/**
 * Base class for tasks that build archives in JAR file format.
 */
@Named("jar")
public class JarArchiver extends ZipArchiver {

    /**
     * The manifest file name.
     */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /**
     * merged manifests added through addConfiguredManifest
     */
    private Manifest configuredManifest;

    /**
     * shadow of the above if upToDate check alters the value
     */
    private Manifest savedConfiguredManifest;

    /**
     * merged manifests added through filesets
     */
    private Manifest filesetManifest;

    /**
     * Manifest of original archive, will be set to null if not in
     * update mode.
     */
    private Manifest originalManifest;

    /**
     * whether to merge fileset manifests;
     * value is true if filesetmanifest is 'merge' or 'mergewithoutmain'
     */
    private FilesetManifestConfig filesetManifestConfig;

    /**
     * whether to merge the main section of fileset manifests;
     * value is true if filesetmanifest is 'merge'
     */
    private boolean mergeManifestsMain = true;

    /**
     * the manifest specified by the 'manifest' attribute *
     */
    private Manifest manifest;

    /**
     * The file found from the 'manifest' attribute. This can be
     * either the location of a manifest, or the name of a jar added
     * through a fileset. If its the name of an added jar, the
     * manifest is looked for in META-INF/MANIFEST.MF
     */
    private File manifestFile;

    /**
     * whether to really create the archive in createEmptyZip, will
     * get set in getResourcesToAdd.
     */
    private boolean createEmpty = false;

    /**
     * Creates a minimal default manifest with {@code Manifest-Version: 1.0} only.
     */
    private boolean minimalDefaultManifest = false;

    /**
     * constructor
     */
    public JarArchiver() {
        super();
        archiveType = "jar";
        setEncoding("UTF8");
    }

    /**
     * Set whether the default manifest is minimal, thus having only {@code Manifest-Version: 1.0} in it.
     *
     * @param minimalDefaultManifest true to create minimal default manifest
     */
    public void setMinimalDefaultManifest(boolean minimalDefaultManifest) {
        this.minimalDefaultManifest = minimalDefaultManifest;
    }

    /**
     * Allows the manifest for the archive file to be provided inline
     * in the build file rather than in an external file.
     *
     * @param newManifest The new manifest
     *
     * @throws ManifestException
     */
    public void addConfiguredManifest(Manifest newManifest) throws ManifestException {
        if (configuredManifest == null) {
            configuredManifest = newManifest;
        } else {
            JdkManifestFactory.merge(configuredManifest, newManifest, false);
        }
        savedConfiguredManifest = configuredManifest;
    }

    /**
     * The manifest file to use. This can be either the location of a manifest, or the name of a jar added through a
     * fileset. If its the name of an added jar, the task expects the manifest to be in the jar at META-INF/MANIFEST.MF.
     *
     * @param manifestFile the manifest file to use.
     *
     * @throws org.codehaus.plexus.archiver.ArchiverException
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setManifest(File manifestFile) throws ArchiverException {
        if (!manifestFile.exists()) {
            throw new ArchiverException("Manifest file: " + manifestFile + " does not exist.");
        }

        this.manifestFile = manifestFile;
    }

    private Manifest getManifest(File manifestFile) throws ArchiverException {
        try (InputStream in = fileInputStream(manifestFile)) {
            return getManifest(in);
        } catch (IOException e) {
            throw new ArchiverException(
                    "Unable to read manifest file: " + manifestFile + " (" + e.getMessage() + ")", e);
        }
    }

    private Manifest getManifest(InputStream is) throws ArchiverException {
        try {
            return new Manifest(is);
        } catch (IOException e) {
            throw new ArchiverException("Unable to read manifest file" + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Behavior when a Manifest is found in a zipfileset or zipgroupfileset file.
     * Valid values are "skip", "merge", and "mergewithoutmain".
     * "merge" will merge all of manifests together, and merge this into any
     * other specified manifests.
     * "mergewithoutmain" merges everything but the Main section of the manifests.
     * Default value is "skip".
     * <p>
     * Note: if this attribute's value is not "skip", the created jar will not
     * be readable by using java.util.jar.JarInputStream</p>
     *
     * @param config setting for found manifest behavior.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setFilesetmanifest(FilesetManifestConfig config) {
        filesetManifestConfig = config;
        mergeManifestsMain = FilesetManifestConfig.merge == config;

        if ((filesetManifestConfig != null) && filesetManifestConfig != FilesetManifestConfig.skip) {

            doubleFilePass = true;
        }
    }

    @Override
    protected void initZipOutputStream(ConcurrentJarCreator zOut) throws ArchiverException, IOException {
        if (!skipWriting) {
            Manifest jarManifest = createManifest();
            writeManifest(zOut, jarManifest);
        }
    }

    @Override
    protected boolean hasVirtualFiles() {
        getLogger().debug("\n\n\nChecking for jar manifest virtual files...\n\n\n");
        System.out.flush();

        return (configuredManifest != null) || (manifest != null) || (manifestFile != null) || super.hasVirtualFiles();
    }

    /**
     * Creates the manifest to be added to the JAR archive.
     * Sub-classes may choose to override this method
     * in order to inspect or modify the JAR manifest file.
     *
     * @return the manifest for the JAR archive.
     *
     * @throws ArchiverException
     */
    protected Manifest createManifest() throws ArchiverException {
        Manifest finalManifest = Manifest.getDefaultManifest(minimalDefaultManifest);

        if ((manifest == null) && (manifestFile != null)) {
            // if we haven't got the manifest yet, attempt to
            // get it now and have manifest be the final merge
            manifest = getManifest(manifestFile);
        }

        /*
         * Precedence: manifestFile wins over inline manifest,
         * over manifests read from the filesets over the original
         * manifest.
         *
         * merge with null argument is a no-op
         */
        if (isInUpdateMode()) {
            JdkManifestFactory.merge(finalManifest, originalManifest, false);
        }
        JdkManifestFactory.merge(finalManifest, filesetManifest, false);
        JdkManifestFactory.merge(finalManifest, configuredManifest, false);
        JdkManifestFactory.merge(finalManifest, manifest, !mergeManifestsMain);

        return finalManifest;
    }

    private void writeManifest(ConcurrentJarCreator zOut, Manifest manifest) throws IOException, ArchiverException {
        for (Enumeration<String> e = manifest.getWarnings(); e.hasMoreElements(); ) {
            getLogger().warn("Manifest warning: " + e.nextElement());
        }

        zipDir(null, zOut, "META-INF/", DEFAULT_DIR_MODE, getEncoding());

        // time to write the manifest
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        manifest.write(baos);
        InputStreamSupplier in = () -> new ByteArrayInputStream(baos.toByteArray());

        super.zipFile(in, zOut, MANIFEST_NAME, System.currentTimeMillis(), null, DEFAULT_FILE_MODE, null, false);
        super.initZipOutputStream(zOut);
    }

    @Override
    protected void finalizeZipOutputStream(ConcurrentJarCreator zOut) throws IOException, ArchiverException {}

    /**
     * Overridden from Zip class to deal with manifests.
     */
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
        if (MANIFEST_NAME.equalsIgnoreCase(vPath)) {
            if (!doubleFilePass || skipWriting) {
                try (InputStream manifestInputStream = is.get()) {
                    filesetManifest(fromArchive, manifestInputStream);
                }
            }
        } else {
            super.zipFile(is, zOut, vPath, lastModified, fromArchive, mode, symlinkDestination, addInParallel);
        }
    }

    private void filesetManifest(File file, InputStream is) throws ArchiverException {
        if ((manifestFile != null) && manifestFile.equals(file)) {
            // If this is the same name specified in 'manifest', this
            // is the manifest to use
            getLogger().debug("Found manifest " + file);
            if (is != null) {
                manifest = getManifest(is);
            } else {
                manifest = getManifest(file);
            }
        } else if ((filesetManifestConfig != null) && filesetManifestConfig != FilesetManifestConfig.skip) {
            // we add this to our group of fileset manifests
            getLogger().debug("Found manifest to merge in file " + file);

            Manifest newManifest;
            if (is != null) {
                newManifest = getManifest(is);
            } else {
                newManifest = getManifest(file);
            }

            if (filesetManifest == null) {
                filesetManifest = newManifest;
            } else {
                JdkManifestFactory.merge(filesetManifest, newManifest, false);
            }
        }
    }

    @Override
    protected boolean createEmptyZip(File zipFile) throws ArchiverException {
        if (!createEmpty) {
            return true;
        }

        try {
            getLogger().debug("Building MANIFEST-only jar: " + getDestFile().getAbsolutePath());
            zipArchiveOutputStream =
                    new ZipArchiveOutputStream(bufferedOutputStream(fileOutputStream(getDestFile(), "jar")));

            zipArchiveOutputStream.setEncoding(getEncoding());
            if (isCompress()) {
                zipArchiveOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
            } else {
                zipArchiveOutputStream.setMethod(ZipArchiveOutputStream.STORED);
            }
            ConcurrentJarCreator ps = new ConcurrentJarCreator(
                    isRecompressAddedZips(), Runtime.getRuntime().availableProcessors());
            initZipOutputStream(ps);
            finalizeZipOutputStream(ps);
        } catch (IOException ioe) {
            throw new ArchiverException("Could not create almost empty JAR archive (" + ioe.getMessage() + ")", ioe);
        } finally {
            // Close the output stream.
            // IOUtil.close( zOut );
            createEmpty = false;
        }
        return true;
    }

    /**
     * Make sure we don't think we already have a MANIFEST next time this task
     * gets executed.
     *
     * @see ZipArchiver#cleanUp
     */
    @Override
    protected void cleanUp() throws IOException {
        super.cleanUp();

        // we want to save this info if we are going to make another pass
        if (!doubleFilePass || !skipWriting) {
            manifest = null;
            configuredManifest = savedConfiguredManifest;
            filesetManifest = null;
            originalManifest = null;
        }
    }

    /**
     * reset to default values.
     *
     * @see ZipArchiver#reset
     */
    @Override
    public void reset() {
        super.reset();
        configuredManifest = null;
        filesetManifestConfig = null;
        mergeManifestsMain = false;
        manifestFile = null;
    }

    public enum FilesetManifestConfig {
        skip,
        merge,
        mergewithoutmain
    }

    /**
     * Override the behavior of the Zip Archiver to match the output of the JAR tool.
     *
     * @param zipEntry to set the last modified time
     * @param lastModifiedTime to set in the zip entry only if {@link #getLastModifiedTime()} returns null
     */
    @Override
    protected void setZipEntryTime(ZipArchiveEntry zipEntry, long lastModifiedTime) {
        if (getLastModifiedTime() != null) {
            lastModifiedTime = getLastModifiedTime().toMillis();
        }

        // The JAR tool does not round up, so we keep that behavior here (JDK-8277755).
        zipEntry.setTime(lastModifiedTime);
    }
}
