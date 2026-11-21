# Extraction paths and symbolic links

Extractors resolve existing path components before checking that output stays
inside the destination directory. Symbolic links are followed before processing
subsequent `..` components, including when the output file does not exist yet.
The same rules resolve the configured destination directory and classify it as
a file or directory.

Existing parent directories are cached within one extraction. Cached entries
are reused only after a fresh check of their filesystem identity without
following symbolic links. Missing paths and symbolic links are not cached, and
the cache is cleared after success or failure. Resolution stops after 64
symbolic-link expansions, rejecting cyclic or excessively long paths.

These path checks apply to the shared extraction code, including TAR and ZIP.
Regular-file and directory entries reject a symbolic link at their destination's
last component, even if it points inside the destination directory or is
dangling. Directory entries colliding with regular files fail rather than
silently retaining those files. Parent-directory creation failures are also
reported. Symbolic-link entries preserve their target text and do not change
the target's permissions or timestamps.

## Optional TAR traversal policy

By default, TAR extractors allow intermediate directory symbolic links that
resolve inside the destination directory. To reject these paths, enable
`failOnSymlinkTraversal` before extraction:

```java
TarUnArchiver extractor = new TarUnArchiver(inputArchive);
extractor.setDestDirectory(outputDirectory);
extractor.setFailOnSymlinkTraversal(true);
extractor.extract();
```

The setting defaults to `false` and is inherited by compressed TAR extractors.
It is a TAR-specific API; the `UnArchiver` interface and provider configuration
interfaces do not expose it. Assembly's `archiverConfig` configures archivers,
rather than extractors.

When enabled, the extractor checks selected, mapped output paths before
overwrite decisions or filesystem changes. It rejects intermediate symbolic
links, including existing links and links created by earlier archive entries.
An intermediate symbolic link appears before the path's last component.
Ordinary symbolic-link entries are still allowed. Excluded entries are skipped.

The configured destination directory is trusted even when its path contains
symbolic links or a symbolic link followed by `..`. Absolute mapped paths can
reach the root through an ancestor alias, such as `/var` referring to
`/private/var` on macOS. A separate alias pointing directly at the destination
root is accepted only in the configured root spelling. Once a path reaches the
root, further intermediate symbolic links are checked even if followed by `..`.
Harmless dot components in either root spelling do not bypass these checks.

An offending entry immediately throws `ArchiverException`, identifying the
archive entry, mapped destination, and symbolic link. Earlier output files can
remain in the destination directory. Physical containment is checked with
either setting; the policy does not require a preliminary archive scan or
temporary storage of archive payloads.
