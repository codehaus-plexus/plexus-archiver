package org.codehaus.plexus.archivers.config;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.archiver.util.AbstractFileSet;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;

final class ResourceCollectionResourceEmitConfigurer implements ResourceEmitConfigurer {

	private final AbstractFileSet<?> fileSet;
	
	public ResourceCollectionResourceEmitConfigurer(AbstractFileSet<?> fileSet) {
		this.fileSet = fileSet;
	}

	@Override
	public void prefix(String prefix) {
		fileSet.setPrefix(prefix);
	}
	
    @Override
    public void setEmptyDirectoryHandling(EmptyDirectoryHandling handling) {
        fileSet.setIncludingEmptyDirectories(
                Objects.requireNonNull(handling, "handling")
                        == EmptyDirectoryHandling.INCLUDE);
    }

    @Override
    public void setFileMappers(Collection<FileMapper> fileMappers) {
        List<FileMapper> mappers = List.copyOf(
                Objects.requireNonNull(fileMappers, "fileMappers"));

        fileSet.setFileMappers(mappers.toArray(FileMapper[]::new));
    }

    @Override
    public void setStreamTransformer(InputStreamTransformer transformer) {
        fileSet.setStreamTransformer(
                Objects.requireNonNull(transformer, "transformer"));
    }
}
