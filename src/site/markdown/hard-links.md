# TAR hard links

A hard link is one more name for the same file. In a TAR archive, a hard-link
member contains a reference to a target member. A data member contains the
contents of a regular file.

## Archive creation

To enable hard-link preservation, call `TarArchiver.setPreserveHardLinks(true)`
before you make the archive:

```java
TarArchiver archiver = new TarArchiver();
archiver.setPreserveHardLinks(true);
archiver.addFileSet(DefaultFileSet.fileSet(inputDirectory));
archiver.setDestFile(outputArchive);
archiver.createArchive();
```

The default value is `false`. With this option enabled, resources with the same
identity and output metadata share one data member. The archiver writes
subsequent names as hard-link members that contain the target name in the
archive. Compressed TAR archivers also have this option.

For hard-link preservation, the resource must implement the optional Plexus IO
`HardLinkIdentitySupplier` interface. A local regular file gives an identity if
it supplies the source file contents and the file system gives a file key. The
archiver writes a full data member for each resource in these conditions:

- The resource uses a custom content supplier.
- A stream transformation changes the contents.
- The resource has no identity.
- The output metadata is different from the target metadata.

A name mapper that changes only the name does not change the identity. It
changes the target name in the archive. The archiver writes symbolic links as
symbolic-link members. If the archiver truncates long names, it disables
hard-link preservation. A truncated target name can identify more than one
member.

To support hard-link preservation, a subclass of `PlexusIoFileResource` or
`TarResource` must supply its own hard-link identity. Resources with that
identity must have the same contents. A subclass can change the contents even if
it uses the same source file or archive member.

The archiver continues hard-link preservation when archive paths go through
symbolic links. GNU tar and bsdtar do the same. A subsequent write through a
directory symbolic link can replace the file at a hard-link target path.

For example, an archive contains these members in this sequence:

1. A data member named `real/file`.
2. A symbolic-link member named `redirect`, with `real` as its target.
3. A data member named `redirect/file`, with different contents.
4. A hard-link member named `alias`, with `real/file` as its target.

With the default extraction settings, `redirect/file` replaces `real/file`
through the symbolic link. The hard link named `alias` then has the replacement
contents. With hard-link preservation disabled, the archiver stores the contents
of each resource in a different data member. The symbolic link does not disable
hard-link preservation for other groups of resources.

## Streaming extraction

The extractor reads the TAR archive one time in a forward direction. It writes
each selected member when it reads that member. It does not scan the headers
first or store file contents in temporary files before extraction.

A hard-link member must reference a previous data member or a chain of previous
hard-link members that ends at a data member. The extractor rejects a selected
member in these conditions:

- The link references a subsequent member.
- The target member is missing.
- The link references itself.
- The target is not a regular file or a hard link to a regular file.
- The path goes outside the destination directory.

The extractor does not search subsequent members or try the rejected members
again.

The extractor uses different records for source archive names and mapped
destination paths. A hard link uses the regular file at the mapped target path.
GNU tar and bsdtar do the same. If a selection rule excludes the target, the
hard link uses the target file in the destination directory. If the overwrite
policy keeps that file, the hard link also uses it. If that file is missing,
extraction stops with an error.

The extractor does not read the archive again to get the contents of an excluded
target. It does not make a destination file for an excluded target.

If a source name occurs more than one time, a hard link references the last
member with that name before the link. The extractor writes members in archive
sequence. If mappers send different names to one destination path, subsequent
hard links use the contents at that path at that time. The extractor does not
change the timestamps or permissions of the target inode when it makes a hard
link. It rejects a hard link with the same destination path as its target. This
is the bsdtar behavior, which is different from GNU tar.

If the extractor cannot make a hard link, it gives an error. It does not copy
the file contents as an alternative. It makes a replacement hard link before it
replaces an output name. After an error, previous output files can stay in the
destination directory.

The protected `AbstractUnArchiver.extractFile` method signature stays available
for members that are not hard links.

### Optional checks for symbolic links

By default, the extractor lets paths go through directory symbolic links to
locations inside the destination directory. GNU tar does the same. To reject
these paths, set `failOnSymlinkTraversal` to `true` before you call `extract()`:

```java
TarUnArchiver extractor = new TarUnArchiver(inputArchive);
extractor.setDestDirectory(outputDirectory);
extractor.setFailOnSymlinkTraversal(true);
extractor.extract();
```

The default value of `failOnSymlinkTraversal` is `false`. Compressed TAR
extractors also have this setting. Assembly's `archiverConfig` configures the
archiver, not the extractor. The `UnArchiver` interface and the provider
configuration interfaces do not have this TAR option.

With this setting enabled, the extractor rejects intermediate symbolic links in
selected, mapped output paths and hard-link target paths. An intermediate
symbolic link is a symbolic link before the last component of a path. The checks
include symbolic links from before extraction and those that previous members
added. The extractor does these checks before overwrite decisions or file system
changes.

The extractor skips excluded members. For a selected hard link, it checks the
mapped target path even if a selection rule excludes the target. If a path has
an intermediate symbolic link, the extractor immediately throws
`ArchiverException`. The error message gives the member path and the symbolic
link path. Previous output files stay at their destination paths.

This setting rejects intermediate symbolic links. This is also the default
bsdtar behavior. Unlike bsdtar, the extractor gives the error immediately. The
extractor does not use the other bsdtar path policies with this setting.

The extractor trusts the configured destination directory, even if its path goes
through a symbolic link. It resolves that directory before it checks paths
inside the directory. This is also true when the configured path contains a
symbolic link followed by `..`. GNU tar and bsdtar also resolve their `-C`
directory before they extract members.

Path resolution follows each existing symbolic link before processing the next
component, including `..`, on all operating systems. It also resolves existing
parents when the destination file does not exist yet. This avoids differences
between Windows JDK versions when canonicalizing a complete path. Resolution
fails after 64 symbolic-link expansions to bound cyclic or excessively long
paths.

An absolute mapped path can reach the root through a symbolic link to an
ancestor directory. For example, macOS uses `/var` as an alias for
`/private/var`. The extractor resolves these ancestor aliases before it reaches
the root. A separate symbolic link to the root itself is accepted only in the
configured root spelling.

After the path reaches the root, the extractor rejects subsequent intermediate
symbolic links. A later `..` component does not restore the ancestor exception.
The checks preserve the remaining path components when they resolve an ancestor
alias.

More `.` components do not change the trusted directory. This is true for the
configured root path and for absolute mapped paths that start at that root. The
extractor also checks intermediate symbolic links before parent components in
member paths.

The extractor accepts symbolic-link members when the setting is `true` or
`false`, preserving their target text and creating them only if the destination
does not already exist. It does not change the target's permissions or timestamps.
Regular-file, directory, and hard-link entries reject a symbolic link at their
destination's last component, including a dangling link. Hard-link targets must
also be regular files, rather than symbolic links. Both traversal settings check
physical containment before creating directories or writing files. The checks
use the file system paths that the extractor finds as it reads each member. An
archive scan, a second read, and temporary storage of file contents are not
necessary for these checks.

## Archived file sets and content access

`TarFile` reads members as the caller requests them. It records each header when
it reads that header. For the current member, it reads contents from the same
stream unless the member is a hard link. For backward hard links, it gets
metadata from the recorded headers without a read of the file contents.

To read contents from a previous member, `TarFile` can open a different cursor
that reads the archive again. The same mechanism can read contents that a caller
read before. For a compressed archive, the cursor also decompresses the data
again. The cursor does not change the position of the member enumeration.

`TarFile` caches one copy of the requested hard-link contents for each data
member. Subsequent hard-link members that reference the same data member use the
same cache. Selectors that read file contents use this mechanism. During
extraction, a second read or file contents cache is necessary only if a caller
requests these contents.

`TarResource` gives the size, bytes, and metadata of the data member for a
backward hard link that meets the target requirements. A caller can select only
the hard-link member from an archived file set. A caller can also copy TAR
members to a different TAR archive or to a ZIP archive. These content requests
get bytes from the archive, not from the destination file system. The size in
the TAR hard-link header stays zero.

Do not open more than one content stream on the same `TarFile` at a time. Do not
use more than one member enumeration on the same `TarFile` at a time.

## Temporary storage

The cache stores one copy of the requested hard-link contents for each data
member. The number of hard links to that data member does not change this rule.
The `close()` method deletes temporary files from the cache.

After you use a reader or resource collection that you manage directly, close
it.

The archiver owns resource collections that a caller adds through
`addArchivedFileSet()` or `addResources()`. It closes these collections and
their archive readers when `createArchive()` completes or stops with an error.
It tries to close each iterator and wrapped collection even if a different close
operation gives an error.

## Maven Assembly configuration

To enable hard-link preservation in Maven Assembly, use this archiver
configuration:

```xml
<archiverConfig>
  <preserveHardLinks>true</preserveHardLinks>
</archiverConfig>
```

The identity API released in Plexus IO `3.8.0` is necessary for hard-link
preservation. If Assembly directly depends on a
previous Plexus IO version, override `plexus-archiver` and `plexus-io` in the
plugin dependencies.

Use a Plexus Archiver version containing this change and Plexus IO `3.8.0` or later.
