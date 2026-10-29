package org.codehaus.plexus.archivers.config;

import java.nio.file.attribute.FileTime;
import java.util.Comparator;

import org.codehaus.plexus.archiver.Archiver;

public interface ReproducibleBuildSettings {
	
	static ReproducibleBuildSettings of(Archiver archiver) {
		return new DefaultReproducibleBuildSettings(archiver);
	}
	
    /**
     * Sets the default last modified time for all archive entries.
     */
    void setDefaultLastModifiedTime(FileTime fileTime);
    
    /**
     * Sets the default uid for all archive entries.
     */
    void setDefaultUid(int uid);
    
    /**
     * Sets the default username for all archive entries.
     */
    void setDefaultUserName(String userName);
    
    /**
     * Sets the default gid for all archive entries.
     */
    void setDefaultGid(int gid);
    
    /**
     * Sets the default group name for all archive entries.
     */
    void setDefaultGroupName(String groupName);
    
    /**
     * Sets the comparator used to sort filenames when scanning directories.
     */
    void setFilenameComparator(Comparator<String> filenameComparator);
}
