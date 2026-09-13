# TAR hard links

`TarArchiver` can preserve hard links when explicitly enabled:

```java
TarArchiver archiver = new TarArchiver();
archiver.setPreserveHardLinks(true);
archiver.addFileSet(DefaultFileSet.fileSet(inputDirectory));
archiver.setDestFile(outputArchive);
archiver.createArchive();
```

The default is `false`. Eligible entries share one data-bearing TAR member;
subsequent aliases contain a hard-link header referring to its final archive name.
The option also applies to the compressed TAR archivers.

Preservation requires a resource implementing Plexus IO's optional
`HardLinkIdentitySupplier`. Local regular files provide an identity when they have
unmodified filesystem contents and the filesystem exposes a file key. Arbitrary
content suppliers, stream transformations, unavailable identities, and conflicting
output metadata result in full regular entries. Transparent name mapping preserves
identity; it changes the archive target name. Symbolic links retain their existing
symbolic-link representation. Truncating long names disables hard-link creation
because a truncated target name may be ambiguous.

Subclasses of `PlexusIoFileResource` and `TarResource` must explicitly supply their
own hard-link identity guarantee. Inheriting a backing file or archive occurrence
does not establish that an overridden content stream contains the same bytes.

Like GNU tar and bsdtar, the writer continues preserving hard links when archive
paths traverse symbolic links. Hard-link headers name paths, so extraction can
link to contents replaced by an intervening write through a directory symlink.
For example, writing `real/file`, `redirect -> real`, a replacement
`redirect/file`, then `alias -> real/file` makes default extraction give `alias`
the replacement contents. Disabling preservation instead stores each resource's
own bytes. Unrelated subsequent hard links remain eligible for preservation.

## Streaming extraction

Extraction reads the TAR in one forward pass, writing each selected member as it
arrives. There is no preliminary header scan or payload staging. Hard links must
refer to earlier regular members or valid backward-link chains. Selected forward
references, missing source targets, self-links, non-regular targets, and escaping
paths are rejected without scanning ahead or retrying later.

Source archive names are tracked separately from mapped destination paths. A link
uses its target's current mapped regular file, as GNU tar and bsdtar do. If the
target was excluded or retained by overwrite policy, that existing destination file
supplies the inode and contents. If it is absent, extraction fails; it does not
reread the archive to recover excluded payloads or create excluded target names.

Duplicate source names bind to the nearest preceding occurrence. Outputs are
applied immediately in archive order. When mappers send different source names to
one destination, later links use that destination's current contents. Link headers
do not overwrite shared inode timestamps or permissions. A same-path link is
rejected, following bsdtar where its behavior differs from GNU tar.

Hard-link creation errors are reported without silently copying a payload for each
alias. Replacement links are created before replacing an existing output name.
The protected `AbstractUnArchiver.extractFile` signature remains available for
ordinary entries. An error can leave earlier members extracted, as in other
streaming extractors; extraction is not transactional.

### Optional symlink-traversal rejection

By default, extraction permits directory symlinks that lead to locations inside
the destination directory, as GNU tar does. To reject traversal through such
links, configure the TAR extractor before calling `extract()`:

```java
TarUnArchiver extractor = new TarUnArchiver(inputArchive);
extractor.setDestDirectory(outputDirectory);
extractor.setFailOnSymlinkTraversal(true);
extractor.extract();
```

`failOnSymlinkTraversal` defaults to `false` and is inherited by the compressed
TAR extractors. This is an extractor setting; Assembly's writer-side
`archiverConfig` does not configure it. The common `UnArchiver` and provider
configuration interfaces do not expose this TAR-specific option.

When enabled, selected, mapped output paths and hard-link target paths cannot
traverse intermediate symbolic links. Checks include pre-existing symlinks and
links created by earlier entries, and occur before overwrite decisions or
filesystem changes. Excluded entries are skipped, but a selected hard link still
checks its excluded target's mapped destination. An offending entry throws
`ArchiverException` immediately with the member and symlink paths; earlier outputs
remain in place. This follows bsdtar's rejection of intermediate symlinks, without
its delayed error reporting or its other pathname policies.

The configured destination directory is trusted even if it is itself reached
through a symlink. The directory is resolved before checking its children, including
when its configured path contains a symlink followed by `..`, as with the utilities'
`-C` directory. Harmless `.` components in the configured root or its absolute
mapped spelling do not change which directory is trusted. Parent components in
member paths remain subject to the intermediate-symlink checks.
Ordinary symlink entries remain allowed; existing rules govern
replacement of final path components. Both settings retain the checks preventing
extraction outside the destination directory. The policy checks encountered
filesystem paths without an archive prescan, replay, or payload staging.

## Archived file sets and content access

`TarFile` enumerates lazily and records header occurrences as they are encountered.
Reading the current ordinary member consumes that same stream. Metadata access
for backward links uses the recorded headers without reading their payloads.

An explicit request for earlier or already-consumed contents may open a separate
replay cursor, including decompression for compressed archives. It does not advance
the entry enumeration. Requested hard-link payloads are cached once per data-bearing
occurrence so subsequent aliases do not each rescan the source. Content-reading
selectors use this same on-demand behavior; ordinary extraction without such
requests needs no replay or payload cache.

`TarResource` exposes a valid backward alias's logical size, bytes, and data-bearing
metadata. This permits selecting an alias alone from an archived file set, repacking
TAR members, or converting them to ZIP. Unlike filesystem extraction, these explicit
content requests obtain bytes from the archive. Low-level TAR link headers still
have size zero. Concurrent content streams and simultaneous enumerations on one
`TarFile` are not supported.

## Temporary storage

Requested hard-link contents are cached once per data-bearing occurrence,
regardless of alias count. Temporary cached payloads are released on `close()`;
callers must close readers and resource collections after use.
For `addArchivedFileSet()` and `addResources()`, the archiver owns the registered
collections and closes them, including their underlying archive readers, when
`createArchive()` finishes or fails. Iterators and wrapped collections are released
even when another resource reports a close failure.

## Maven Assembly configuration

The writer option can be passed through Assembly's existing archiver configuration:

```xml
<archiverConfig>
  <preserveHardLinks>true</preserveHardLinks>
</archiverConfig>
```

This development change depends on the companion Plexus IO `3.7.1-SNAPSHOT`
identity API. When testing an Assembly version that directly depends on an older
Plexus IO, override both `plexus-archiver` and `plexus-io` in the plugin's dependencies.
Use released versions containing both changes once available; the companion
snapshot must be built locally until then.
