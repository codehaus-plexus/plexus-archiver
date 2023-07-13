package org.codehaus.plexus.archiver.jar;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JarArchiverTest extends BaseJarArchiverTest {

    @TempDir
    private Path tempDir;

    @Test
    void testCreateManifestOnlyJar() throws IOException, ManifestException, ArchiverException {
        File jarFile = Files.createTempFile(tempDir, "JarArchiverTest.", ".jar").toFile();

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile(jarFile);

        Manifest manifest = new Manifest();
        Manifest.Attribute attribute =
                new Manifest.Attribute("Main-Class", getClass().getName());

        manifest.addConfiguredAttribute(attribute);

        archiver.addConfiguredManifest(manifest);

        archiver.createArchive();
    }

    @Test
    void testNonCompressed() throws IOException, ManifestException, ArchiverException {
        File jarFile = new File("target/output/jarArchiveNonCompressed.jar");

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile(jarFile);
        archiver.setCompress(false);
        archiver.addDirectory(new File("src/test/resources/mjar179"));
        archiver.createArchive();
    }

    @Test
    void testVeryLargeJar() throws IOException, ArchiverException {
        // Generate some number of random files that is likely to be
        // two or three times the number of available file handles
        Random rand = new Random();
        for (int i = 0; i < 45000; i++) {
            Path path = tempDir.resolve("file" + i);
            try (OutputStream out = Files.newOutputStream(path)) {
                byte[] data = new byte[512]; // 512bytes per file
                rand.nextBytes(data);
                out.write(data);
                out.flush();
            }
        }

        File jarFile = new File("target/output/veryLargeJar.jar");

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile(jarFile);
        archiver.addDirectory(tempDir.toFile());
        archiver.createArchive();
        // Clean up
        Files.delete(jarFile.toPath());
    }

    @Test
    void testReproducibleBuild() throws IOException, ManifestException, ParseException {
        String[] tzList = {
            "America/Managua",
            "America/New_York",
            "America/Buenos_Aires",
            "America/Sao_Paulo",
            "America/Los_Angeles",
            "Africa/Cairo",
            "Africa/Lagos",
            "Africa/Nairobi",
            "Europe/Lisbon",
            "Europe/Madrid",
            "Europe/Moscow",
            "Europe/Oslo",
            "Australia/Sydney",
            "Asia/Tokyo",
            "Asia/Singapore",
            "Asia/Qatar",
            "Asia/Seoul",
            "Atlantic/Bermuda",
            "UTC",
            "GMT",
            "Etc/GMT-14"
        };
        for (String tzId : tzList) {
            // Every single run with different Time Zone should set the same modification time.
            createReproducibleBuild(tzId);
        }
    }

    private void createReproducibleBuild(String timeZoneId) throws IOException, ManifestException, ParseException {
        final TimeZone defaultTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
        try {
            String tzName = timeZoneId.substring(timeZoneId.lastIndexOf('/') + 1);
            Path jarFile = Files.createTempFile(tempDir, "JarArchiverTest-" + tzName + "-", ".jar");

            Manifest manifest = new Manifest();
            Manifest.Attribute attribute = new Manifest.Attribute("Main-Class", "com.example.app.Main");
            manifest.addConfiguredAttribute(attribute);

            JarArchiver archiver = getJarArchiver();
            archiver.setDestFile(jarFile.toFile());

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            long parsedTime = isoFormat.parse("2038-01-19T03:14:08Z").getTime();
            FileTime lastModTime = FileTime.fromMillis(parsedTime);

            archiver.configureReproducibleBuild(lastModTime);

            archiver.addConfiguredManifest(manifest);
            archiver.addDirectory(new File("src/test/resources/java-classes"));

            archiver.createArchive();

            // zip 2 seconds precision, normalized to UTC
            long expectedTime = normalizeLastModifiedTime(parsedTime - (parsedTime % 2000));
            try (ZipFile zip = new ZipFile(jarFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    long time = entry.getTime();
                    assertEquals(expectedTime, time, "last modification time does not match");
                }
            }
        } finally {
            TimeZone.setDefault(defaultTz);
        }
    }

    /**
     * Check group not writable for reproducible archive.
     *
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testReproducibleUmask() throws IOException, ParseException {
        Path jarFile = Files.createTempFile(tempDir, "JarArchiverTest-umask", ".jar");

        JarArchiver archiver = getJarArchiver();
        archiver.setDestFile(jarFile.toFile());

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        long parsedTime = isoFormat.parse("2038-01-19T03:14:08Z").getTime();
        FileTime lastModTime = FileTime.fromMillis(parsedTime);

        archiver.configureReproducibleBuild(lastModTime);

        archiver.addDirectory(new File("src/test/resources/java-classes"));
        archiver.addFile(new File("src/test/resources/world-writable/foo.txt"), "addFile.txt");

        archiver.createArchive();

        try (org.apache.commons.compress.archivers.zip.ZipFile zip =
                new org.apache.commons.compress.archivers.zip.ZipFile(jarFile.toFile())) {
            Enumeration<? extends ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                int mode = entry.getUnixMode();
                assertEquals(
                        0,
                        mode & 0_020,
                        entry.getName() + " group should not be writable in reproducible mode: "
                                + Integer.toOctalString(mode));
            }
        }
    }

    @Override
    protected JarArchiver getJarArchiver() {
        return new JarArchiver();
    }
}
