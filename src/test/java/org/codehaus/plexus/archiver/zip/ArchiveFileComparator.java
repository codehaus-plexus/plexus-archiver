package org.codehaus.plexus.archiver.zip;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.codehaus.plexus.archiver.tar.TarFile;

/**
 * A utility class, which allows to compare archive files.
 */
public final class ArchiveFileComparator
{

    /**
     * Iterate over the TarFile and run the consumer on each TarArchiveEntry.
     */
    public static void forEachTarArchiveEntry( TarFile file, Consumer<TarArchiveEntry> consumer )
    {
        try
        {
            Collections.list( file.getEntries() )
                .stream()
                .filter( entry -> !entry.isDirectory() )
                .map( TarArchiveEntry.class::cast )
                .forEachOrdered( consumer );
        }
        catch ( IOException ioe )
        {
            throw new UncheckedIOException( ioe );
        }
    }

    private static void forEachZipArchiveEntry( ZipFile file, Consumer<ZipArchiveEntry> consumer )
    {
        Collections.list( file.getEntries() )
            .stream()
            .filter( entry -> !entry.isDirectory() )
            .forEachOrdered( consumer );
    }

    /**
     * Creates a map with the archive files contents. The map keys are the names of file entries in the archive file.
     * The map values are the respective archive entries.
     */
    private static Map<String, TarArchiveEntry> getFileEntries( TarFile file )
    {
        try
        {
            return Collections.list( file.getEntries() )
                .stream()
                .filter( entry -> !entry.isDirectory() )
                .map( TarArchiveEntry.class::cast )
                .collect( Collectors.toMap( ArchiveEntry::getName, Function.identity() ) );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    /**
     * Creates a map with the archive files contents. The map keys are the names of file entries in the archive file.
     * The map values are the respective archive entries.
     */
    private static Map<String, ZipArchiveEntry> getFileEntries( ZipFile file )
    {
        return Collections.list( file.getEntries() )
            .stream()
            .filter( entry -> !entry.isDirectory() )
            .collect( Collectors.toMap( ArchiveEntry::getName, Function.identity() ) );
    }

    private static void assertTarEquals( TarFile file1, TarArchiveEntry entry1,
                                         TarFile file2, TarArchiveEntry entry2 )
    {
        assertThat( entry1.isDirectory() ).isEqualTo( entry2.isDirectory() );
        assertThat( entry1.isSymbolicLink() ).isEqualTo( entry2.isSymbolicLink() );
        assertThat( entry1.getLastModifiedDate() ).isEqualTo( entry2.getLastModifiedDate() );
        assertThat( entry1.getUserName() ).isEqualTo( entry2.getUserName() );
        assertThat( entry1.getGroupName() ).isEqualTo( entry2.getGroupName() );

        try ( InputStream is1 = file1.getInputStream( entry1 );
              InputStream is2 = file2.getInputStream( entry2 ); )
        {
            assertThat( is1 )
                .as( "Content of the entry1 %s is different of entry2 %s", entry1.getName(), entry2.getName() )
                .hasSameContentAs( is2 );
        }
        catch ( IOException ioe )
        {
            throw new UncheckedIOException( ioe );
        }
    }

    private static void assertTarZipEquals( TarFile file1, TarArchiveEntry entry1,
                                            ZipFile file2, ZipArchiveEntry entry2 )
    {
        assertThat( entry1.isDirectory() ).isEqualTo( entry2.isDirectory() );
        assertThat( entry1.getLastModifiedDate() ).isCloseTo( entry2.getLastModifiedDate(), 1000 );

        try ( InputStream is1 = file1.getInputStream( entry1 );
              InputStream is2 = file2.getInputStream( entry2 ); )
        {
            assertThat( is1 )
                .as( "Content of the entry1 %s is different of entry2 %s", entry1.getName(), entry2.getName() )
                .hasSameContentAs( is2 );
        }
        catch ( IOException ioe )
        {
            throw new UncheckedIOException( ioe );
        }
    }

    private static void assertZipEquals( ZipFile file1, ZipArchiveEntry entry1,
                                         ZipFile file2, ZipArchiveEntry entry2 )
    {
        assertThat( entry1.isDirectory() ).isEqualTo( entry2.isDirectory() );
        assertThat( entry1.isUnixSymlink() ).isEqualTo( entry2.isUnixSymlink() );
        assertThat( entry1.getLastModifiedDate() ).isCloseTo( entry2.getLastModifiedDate(), 1000 );

        try ( InputStream is1 = file1.getInputStream( entry1 );
              InputStream is2 = file2.getInputStream( entry2 ); )
        {
            assertThat( is1 )
                .as( "Content of the entry1 %s is different of entry2 %s", entry1.getName(), entry2.getName() )
                .hasSameContentAs( is2 );
        }
        catch ( IOException ioe )
        {
            throw new UncheckedIOException( ioe );
        }
    }

    /**
     * Called to compare the given Tar files.
     */
    public static void assertTarEquals( final TarFile file1, final TarFile file2, final String prefix )
    {
        final Map<String, TarArchiveEntry> map2 = getFileEntries( file2 );

        forEachTarArchiveEntry( file1, entry1 -> {
            final String name2 = getNameWithPrefix( prefix, entry1.getName() );
            final TarArchiveEntry entry2 = map2.remove( name2 );
            assertThat( entry2 ).as( "Missing entry in file2: %s", name2 ).isNotNull();
            assertTarEquals( file1, entry1, file2, entry2 );
        } );

        assertThat( map2 ).as("Found additional entries in file2").isEmpty();
    }

    /**
     * Called to compare the given Tar and Zip files.
     */
    public static void assertTarZipEquals( final TarFile file1, final ZipFile file2, final String prefix )
    {
        final Map<String, ZipArchiveEntry> map2 = getFileEntries( file2 );

        forEachTarArchiveEntry( file1, entry1 -> {
            final String name2 = getNameWithPrefix( prefix, entry1.getName() );
            final ZipArchiveEntry entry2 = map2.remove( name2 );
            assertThat( entry2 ).as( "Missing entry in file2: %s", name2 ).isNotNull();
            assertTarZipEquals( file1, entry1, file2, entry2 );
        } );

        assertThat( map2 ).as("Found additional entries in file2").isEmpty();
    }

    /**
     * Called to compare the given Zip files.
     */
    public static void assertZipEquals( final ZipFile file1, final ZipFile file2, final String prefix )
    {
        final Map<String, ZipArchiveEntry> map2 = getFileEntries( file2 );

        forEachZipArchiveEntry( file1, entry1 -> {
            final String name2 = getNameWithPrefix( prefix, entry1.getName() );
            final ZipArchiveEntry entry2 = map2.remove( name2 );
            assertThat( entry2 ).as( "Missing entry in file2: %s", name2 ).isNotNull();
            assertZipEquals( file1, entry1, file2, entry2 );
        } );

        assertThat( map2 ).as("Found additional entries in file2").isEmpty();
    }

    private static String getNameWithPrefix( String prefix, String name )
    {
        return prefix == null ? name : prefix + name;
    }

}
