package org.codehaus.plexus.archiver.manager;

import javax.annotation.Nonnull;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

import org.codehaus.plexus.util.StringUtils;

class FileNames {

    private FileNames() {}

    @Nonnull
    static String getFileExtension(@Nonnull Path path) {
        return getFileExtension(path.getFileName().toString());
    }

    @Nonnull
    static String getFileExtension(@Nonnull File file) {
        return getFileExtension(file.getName());
    }

    @Nonnull
    private static String getFileExtension(@Nonnull String name) {
        String fileName = name.toLowerCase(Locale.ROOT);
        String[] tokens = StringUtils.split(fileName, ".");

        String archiveExt = "";

        if (tokens.length == 2) {
            archiveExt = tokens[1];
        } else if (tokens.length > 2 && "tar".equals(tokens[tokens.length - 2])) {
            archiveExt = "tar." + tokens[tokens.length - 1];
        } else if (tokens.length > 2) {
            archiveExt = tokens[tokens.length - 1];
        }

        return archiveExt;
    }
}
