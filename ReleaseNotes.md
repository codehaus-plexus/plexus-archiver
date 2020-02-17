Plexus Archiver Release Notes
========================================================================

Plexus Archiver 4.2.1
---------------------

### Bugs

 * [Issue #126][issue-126] - Fixed broken javadoc for
 `Archiver#configureReproducible`.
 * [Issue #127][issue-127] - Fixed reproducible zip entry time depends 
 on local daylight saving time.

Plexus Archiver 4.2.0
---------------------

### Improvements

 * [Pull Request #121][pr-121] - Add API to configure reproducible archives -
 `Archiver#configureReproducible`.
 * Add option to force the user and group for all archive entries.
 * Add option to force the last modified date for all archive entries.
 * [Issue #114][issue-114] - Add option to provide `Comparator` for `Archiver`.
 The archive entries will be added in the order specified by the
 provided comparator.
 * [Pull Request #117][pr-117] - Add option to limit the output size for
 `AbstractZipUnArchiver` as a way of protection against ZIP bombs.
 Thanks to Sergey Patrikeev and Semyon Atamas.
 * Various code improvements. Thanks to Semyon Atamas and
 Sergey Patrikeev.

### Bugs

 * [Issue #94][issue-94] - Fixed setting archiver destination to
 the working directory causes `NullPointerException`.

### Tasks

 * [Issue #119][issue-119] - Updated dependencies: `commons-compress` to 1.18,
 `plexus-io` to 3.2.0 and `plexus-utils` to 3.3.0.

Plexus Archiver 4.1.0
---------------------

### Improvements

 * [Issue #110][issue-110] - Add option to omit "Created-By" manifest entry.

Plexus Archiver 4.0.0
---------------------

**NOTE:** Because 3.7.0 introduced backward incompatible changes
in the API (new methods in interfaces) we bumped the Plexus Archiver
version to 4.0.0.

Plexus Archiver 4.0.0 requires Java 7.

### Improvements
 * [Issue #105][issue-105] - Fixed performance regression introduced
 in Plexus Archiver 3.0.2. Affected are systems where the retrieval of
 a file's user and group attributes is costly operation.
 * [Pull Request #106][pr-106] - `PlexusIoZipFileResourceCollection`
 performance is improved when working with signed Jar files.

### Tasks

 * [Issue #108][issue-108], [Issue #109][issue-109] - Updated dependencies:
 `plexus-io` to 3.1.1 and `plexus-utils` to 3.1.1.

Plexus Archiver 3.7.0
---------------------

Plexus Archiver 3.7.0 requires Java 7.

### New Features
 * [Pull Request #92][pr-92] - `BaseFileSet` now accepts an array of
 `FileMapper` instances, allowing the name and/or the path of
 entries in the archive to be modified. Thanks to Thomas Collignon.
 * [Pull Request #100][pr-100] - `UnArchiver` now accepts an array of
 `FileMapper` instances, allowing the name and/or the path of the
 extracted entries to be modified. Thanks to Markus Karg.


### Improvements

 * [Issue #98][issue-98] - Now `JarToolModularJarArchiver` does not copy
 the module descriptors (`module-info.class`) to temporary location.
 It adds them directly to the JAR archive.
 * [Issue #101][issue-101], [Pull Request #102][pr-102] - Now `ModularJarArchiver`
 implementations will use the JAR file manifest `Main-Class` attribute
 as module main class if one is not excellently set.

### Bugs

 * [Issue #95][issue-95] - Fixed ZIP entries last modification time
 rounded down on Java 8 or later
 * [Issue #97][issue-97] - Fixed `module-info.class` not being added
 to the modular JAR index file.

### Tasks

 * [Issue #103][issue-103], [Issue #104][issue-104] - Updated dependencies:
 `commons-compress` to 1.18 and `plexus-io` to 3.1.0.

Plexus Archiver 3.6.0
---------------------

Plexus Archiver 3.6.0 requires Java 7. 

### New Features

 * [Pull Request #84][pr-84], [Issue #57][issue-68] - Added Archiver implementation
 (`JarToolModularJarArchiver`) that creates modular JAR files using the JDK jar tool.
 The implementation uses `java.util.spi.ToolProvider` introduced in Java 9,
 so if it is run on Java 7 or 8 the resulting archive will be identical to a
 one created by `JarArchiver` - the module descriptor is not going to be validated
 and no additional information (such as version, main class and packages)
 is going to be added.
 * [Issue #67][issue-67] - Added ability to set the module version and main class
 of a modular JAR file
 * [Pull Request #83][pr-83] - Added new protected method (`postCreateArchive`)
 to `AbstractArchiver` that is called after the archive is created successfully

### Improvements

 * [Pull Request #87][pr-87] - of Levan Giguashvili (odinn1984)
   Snyk eng team to fix a possible security issue.
   (See https://gist.github.com/grnd/eafd7dab7c4cc6197d817a07fa46b2df)

### Bugs

 * [Pull Request #73][pr-73] - Symbolic links not properly encoded
 in ZIP archives
 * [Issue #57][issue-57] - `ZipArchiver` creates archives with inconsistent
 central directory entries
 * [Issue #79][issue-79] - `JarArchiver` and `PlexusIoZipFileResourceCollection`
 leak file descriptors

### Tasks

 * [Pull Request #77][pr-77] - Fixed the way unit tests modify
 the timestamp of a file
 * [Issue #71][issue-71], [Pull Request #72][pr-72], [Issue #76][issue-76],
 [Issue #78][issue-78], [Issue #85][issue-85], [Issue #86][issue-86] -
 Updated dependencies: `commons-compress` to 1.16.1, `plexus-utils` to 3.1.0,
 `org.tukaani:xz` to 1.8 and `plexus-io` to 3.0.1


Plexus Archiver 3.5
-------------------

Plexus Archiver 3.5 requires Java 7. Now Plexus Archiver uses pure Java
implementations to deal with file attributes so the `useJvmChmod` is no
longer used and it is just ignored. `Archiver#setUseJvmChmod`,
`Archiver#isUseJvmChmod()`, `UnArchiver#setUseJvmChmod`,
`UnArchiver#isUseJvmChmod()`,
`ArchiveEntryUtils#chmod( File, int, Logger, boolean )` and
`ArchiveEntryUtils#chmod( File, int, Logger )` are deprecated and are
subject to removal in a future version.

### Improvements

 * [Pull Request #51][pr-51] - More specific exception for cases when
   there are no files to archive. Now `EmptyArchiveException` is thrown
   when you try to create empty archive. Previously the more generic
   `ArchiverException` was thrown.

### Bugs

 * [Issue #47][issue-47] - Archiver follows symlinks on Windows
 * [Issue #53][issue-53] - `AbstractZipArchiver` no longer respects
   `recompressAddedZips`
 * [Issue #58][issue-58] - Creates corrupt JARs

### Tasks

 * [Pull Request #56][pr-56] - Upgrade the minimum required Java version to 7
   and Plexus IO to 3.0.0
 * [Issue #60][issue-60] - Upgrade dependencies.
   `plexus-container-default` to `1.0-alpha-30`,
   `commons-compress` to 1.14, `org.tukaani.xz` to 1.6 and
   `com.google.code.findbugs.jsr305` to 3.0.2

Plexus Archiver 3.4.1
-------------------

### Improvements

 * [Pull Request #87][pr-87] - of Levan Giguashvili (odinn1984)
   Snyk eng team to fix a possible security issue.
   (See https://gist.github.com/grnd/eafd7dab7c4cc6197d817a07fa46b2df)

Plexus Archiver 3.4
-------------------

### Bugs

 * [Issue #45][issue-45] - Default `ArchiveManager` does not support
   `tar.xz` and `tar.snappy` file extensions
 * [Issue #46][issue-46] - `JarSecurityFileSelector` needs to support
   signature files ending in `.EC` or `.ec`

Plexus Archiver 3.3
-------------------

### Improvements

 * [Issue #42][issue-42] - No need to fallback to unicode path extra field
   policy `NOT_ENCODEABLE`
 * [Issue #39][issue-39] - Updated to stop falling back to the unicode path extra field
   policy `NOT_ENCODEABLE`. If a name is not encodeable in UTF-8, it also is not
   encodeable in the extra field.
   Updated to always add the Info-ZIP Unicode Path Extra Field when creating an
   archive using an encoding different from UTF-8 instead of only when a name is
   not encodeable. Additionally support that extra field when unarchiving.
 * [Issue #38][issue-38] - Downgrade `PrintWriter` to `Writer` in `Manifest`
 * [Issue #36][issue-36] - `Created-by` entry does not reflect who created the JAR
 * [Issue #35][issue-35] - Replace `defaultManifest.mf` with inline code
 * [Issue #17][issue-17] - Remove unnecessary conversion in `Manifest#Attribute#write`
 * [Issue #16][issue-16] - Manifest entry `Archiver-Version` is incomplete/wrong.
   Entry does not reflect the archiver version. Remove since it
   adds not information it pretends to add.
 * [Issue #5][issue-5] - Added proper bound on memory usage, patch by Björn Eickvonder
 * [Pull Request #41][pr-41] - Support the Info-ZIP Unicode Path Extra Field.

### Bugs

 * [Issue #43][issue-43] - Updated to stop failing creating `Created-by` manifest entries,
   when the version of the archiver cannot be determined
 * [Issue #37][issue-37] - Deprecate `Manifest(Reader)` and update all related
   Implemenation does not properly map characters to map and makes assumptions
   about character encoding which might lead to failures.
   Deprecate and rely on Java Manifest reader to do the right thing.
 * [Issue #20][issue-20] - `Manifest#write` blindly casts bytes to chars
 * [Issue #18][issue-18] - `Manifest#Attribute#writeLine` does not properly calculate
   max line length

### Tasks

 * [Issue #40][issue-40] - Updated to upgrade `plexus-utils` to latest patch release

Plexus Archiver 3.2
-------------------

### New Features

 * [Pull Request #27][pr-27] - Added xz compression support

### Improvements

 * [Issue #33][issue-33] - Exceptions are suppressed incorrectly

### Tasks

 * [Issue #31][issue-31] - Upgrade of `plexus-utils` to 3.0.23
 * [Issue #32][issue-32] - Upgrade of `commons-io` to 2.5

Plexus Archiver 3.1.1
---------------------

### Bugs

 * [Issue #28][issue-28] - which checks for null preventing NPE

### Improvements

 * [Pull Request #26][pr-26] - Improvement from Plamen Totev

Plexus Archiver 3.0.2
---------------------

### Improvement

 * `DirectoryArhiver` now respects filemode for directories.
   Thanks for Olivier Fayau for patch.

Plexus Archiver 3.0.1
---------------------

### Improvements

 * [Issue #3][issue-3] - Switched to pure-java snappy

Plexus Archiver 3.0
-------------------

### Improvements

 * PLXCOMP-282 - Add Snappy compression support

Plexus Archiver 2.10.3
----------------------

### Bugs

 * [Issue #6][issue-6] - "Too many open files" when building large jars

Plexus Archiver 2.10.2
----------------------

### Bugs

 * https://issues.apache.org/jira/browse/MASSEMBLY-769

Plexus Archiver 2.10.1
----------------------

### Bugs

 * https://issues.apache.org/jira/browse/MASSEMBLY-768

Plexus Archiver 2.10
--------------------

 * Symlink support in DirectoryArchiver
 * Multithreaded ZIP support
 * Fixed resource leak on ZIP files included in ZIP files.
 * Added encoding supporting overload: addArchivedFileSet( final ArchivedFileSet fileSet, Charset charset )
 * Fixed NPE with missing folder in TAR
 * Moved all "zip" support to archiver (from io).

Plexus Archiver 2.9.1
---------------------

 * Wrap-up release with plexus-io-2.4.1

Plexus Archiver 2.9
-------------------

### Improvements

 * PLXCOMP-276 - Reduce number of ways to create a PlexusIoResource

### Bugs

 * PLXCOMP-277 - Archiver unable to determine file equailty

Plexus Archiver 2.8.4
---------------------

### Bugs

 * PLXCOMP-273 - Normalize file separators for duplicate check

Plexus Archiver 2.8.3
---------------------

### Bugs

 * PLXCOMP-271 - Implicit created directories do not obey proper dirMode
 * PLXCOMP-272 - Overriding dirmode/filemode breaks symlinks

Plexus Archiver 2.8.2
---------------------

### Bugs

 * PLXCOMP-266 - In-place filtering of streams give incorrect content length
   for tar files

Plexus Archiver 2.8.1
---------------------

### Improvements

 * PLXCOMP-268 - Add diagnostic archivers

Plexus Archiver 2.8
-------------------

### New Features

 * PLXCOMP-263 - Support on-the fly stream filtering

### Improvements

 * PLXCOMP-255 - Removed dependency plexus-container-default:1.0-alpha-9-stable-1

### Bugs

 * PLXCOMP-262 - Directory symlinks in zip files are incorrect

Plexus Archiver 2.7.1
---------------------

### Improvements

 * PLXCOMP-257 - Inconsistent buffering

### Bugs

 * PLXCOMP-256 - Several archivers leaks file handles

Plexus Archiver 2.7
-------------------

### Improvements

 * PLXCOMP-253 - Switch default encoding to UTF-8

### Bugs

 * PLXCOMP-252 - Tar archivers cannot roundtrip own archives on windows, UTF8 bug

Plexus Archiver 2.6.4
---------------------

### Bugs

 * PLXCOMP-45 - ignoreWebXML flag use is opposite of what the name implies
 * PLXCOMP-107 - Fail to unzip archive, which contains file with name
   'How_can_I_annotate_a_part_in_the_AAM%3F.Help'
 * PLXCOMP-234 - Plexus archiver TarOptions setDirMode and setMode do not do
   anything unless TarArchiver.setOptions is called

Plexus Archiver 2.6.3
---------------------

### Bugs

* PLXCOMP-233 - Plexus archiver can create tarfiles with empty uid and gid bytes
* PLXCOMP-247 - Bug with windows AND java5

Plexus Archiver 2.6.2
---------------------

### Bugs

  * PLXCOMP-238 - CRC Failure if compress=false and file size <= 4 bytes
  * PLXCOMP-245 - Archives created on windows get zero permissions,
    creates malformed permissions on linux

Plexus Archiver 2.6.1
---------------------

### Bugs

 * PLXCOMP-243 - Restore JDK1.5 compatibility

Plexus Archiver 2.6
-------------------

### Bugs

 * PLXCOMP-113 - zip unarchiver doesn't support symlinks (and trivial to fix)

### Improvement

 * PLXCOMP-64 - add symlink support to tar unarchiver
 * PLXCOMP-117 - add symbolic links managment

Plexus Archiver 2.5
-------------------

### Improvements

 * PLXCOMP-153 - TarUnArchiver does not support includes/excludes
 * PLXCOMP-240 - Convert everything to commons-compress

### Bugs

 * PLXCOMP-13 - Plexus Archiver fails on certain Jars
 * PLXCOMP-205 - Tar unarchiver does not respect includes/excludes flags
 * PLXCOMP-216 - Unarchiver extracts files into wrong directory
 * PLXCOMP-232 - Failures to unpack .tar.gz files
 * PLXCOMP-236 - ZipUnArchiver fails to extract large (>4GB) ZIP files

Plexus Archiver 2.4.4
---------------------

### Bugs

 * PLXCOMP-178 - last modification time is not preserved
 * PLXCOMP-222 - ZipOutputStream does not set Language encoding flag (EFS)
   when using UTF-8 encoding
 * PLXCOMP-226 - Bug in org.codehaus.plexus.archiver.zip.ZipOutputStream.closeEntry
   (ZipOutputStream.java:352)

[issue-3]: https://github.com/codehaus-plexus/plexus-archiver/issues/3
[issue-5]: https://github.com/codehaus-plexus/plexus-archiver/issues/5
[issue-6]: https://github.com/codehaus-plexus/plexus-archiver/issues/6
[issue-16]: https://github.com/codehaus-plexus/plexus-archiver/issues/16
[issue-17]: https://github.com/codehaus-plexus/plexus-archiver/issues/17
[issue-18]: https://github.com/codehaus-plexus/plexus-archiver/issues/18
[issue-20]: https://github.com/codehaus-plexus/plexus-archiver/issues/20
[issue-28]: https://github.com/codehaus-plexus/plexus-archiver/issues/28
[issue-31]: https://github.com/codehaus-plexus/plexus-archiver/issues/31
[issue-32]: https://github.com/codehaus-plexus/plexus-archiver/issues/32
[issue-33]: https://github.com/codehaus-plexus/plexus-archiver/issues/32
[issue-34]: https://github.com/codehaus-plexus/plexus-archiver/issues/34
[issue-35]: https://github.com/codehaus-plexus/plexus-archiver/issues/35
[issue-36]: https://github.com/codehaus-plexus/plexus-archiver/issues/36
[issue-37]: https://github.com/codehaus-plexus/plexus-archiver/issues/37
[issue-38]: https://github.com/codehaus-plexus/plexus-archiver/issues/38
[issue-39]: https://github.com/codehaus-plexus/plexus-archiver/issues/39
[issue-40]: https://github.com/codehaus-plexus/plexus-archiver/issues/40
[issue-42]: https://github.com/codehaus-plexus/plexus-archiver/issues/42
[issue-43]: https://github.com/codehaus-plexus/plexus-archiver/issues/43
[issue-45]: https://github.com/codehaus-plexus/plexus-archiver/issues/45
[issue-46]: https://github.com/codehaus-plexus/plexus-archiver/issues/46
[issue-47]: https://github.com/codehaus-plexus/plexus-archiver/issues/47
[issue-53]: https://github.com/codehaus-plexus/plexus-archiver/issues/53
[issue-57]: https://github.com/codehaus-plexus/plexus-archiver/issues/57
[issue-58]: https://github.com/codehaus-plexus/plexus-archiver/issues/58
[issue-60]: https://github.com/codehaus-plexus/plexus-archiver/issues/60
[issue-67]: https://github.com/codehaus-plexus/plexus-archiver/issues/67
[issue-68]: https://github.com/codehaus-plexus/plexus-archiver/issues/68
[issue-71]: https://github.com/codehaus-plexus/plexus-archiver/issues/71
[issue-76]: https://github.com/codehaus-plexus/plexus-archiver/issues/76
[issue-78]: https://github.com/codehaus-plexus/plexus-archiver/issues/78
[issue-79]: https://github.com/codehaus-plexus/plexus-archiver/issues/79
[issue-85]: https://github.com/codehaus-plexus/plexus-archiver/issues/85
[issue-86]: https://github.com/codehaus-plexus/plexus-archiver/issues/86
[issue-94]: https://github.com/codehaus-plexus/plexus-archiver/issues/94
[issue-95]: https://github.com/codehaus-plexus/plexus-archiver/issues/95
[issue-97]: https://github.com/codehaus-plexus/plexus-archiver/issues/97
[issue-98]: https://github.com/codehaus-plexus/plexus-archiver/issues/98
[issue-101]: https://github.com/codehaus-plexus/plexus-archiver/issues/101
[issue-103]: https://github.com/codehaus-plexus/plexus-archiver/issues/103
[issue-104]: https://github.com/codehaus-plexus/plexus-archiver/issues/104
[issue-105]: https://github.com/codehaus-plexus/plexus-archiver/issues/105
[issue-108]: https://github.com/codehaus-plexus/plexus-archiver/issues/108
[issue-109]: https://github.com/codehaus-plexus/plexus-archiver/issues/109
[issue-110]: https://github.com/codehaus-plexus/plexus-archiver/issues/110
[issue-114]: https://github.com/codehaus-plexus/plexus-archiver/issues/114
[issue-119]: https://github.com/codehaus-plexus/plexus-archiver/issues/119
[issue-126]: https://github.com/codehaus-plexus/plexus-archiver/issues/126
[issue-127]: https://github.com/codehaus-plexus/plexus-archiver/issues/127
[pr-26]: https://github.com/codehaus-plexus/plexus-archiver/issues/26
[pr-27]: https://github.com/codehaus-plexus/plexus-archiver/issues/27
[pr-41]: https://github.com/codehaus-plexus/plexus-archiver/pull/41
[pr-51]: https://github.com/codehaus-plexus/plexus-archiver/pull/51
[pr-56]: https://github.com/codehaus-plexus/plexus-archiver/pull/56
[pr-72]: https://github.com/codehaus-plexus/plexus-archiver/pull/72
[pr-73]: https://github.com/codehaus-plexus/plexus-archiver/pull/73
[pr-77]: https://github.com/codehaus-plexus/plexus-archiver/pull/77
[pr-83]: https://github.com/codehaus-plexus/plexus-archiver/pull/83
[pr-84]: https://github.com/codehaus-plexus/plexus-archiver/pull/84
[pr-87]: https://github.com/codehaus-plexus/plexus-archiver/pull/87
[pr-92]: https://github.com/codehaus-plexus/plexus-archiver/pull/92
[pr-100]: https://github.com/codehaus-plexus/plexus-archiver/pull/100
[pr-102]: https://github.com/codehaus-plexus/plexus-archiver/pull/102
[pr-106]: https://github.com/codehaus-plexus/plexus-archiver/pull/106
[pr-117]: https://github.com/codehaus-plexus/plexus-archiver/pull/117
[pr-121]: https://github.com/codehaus-plexus/plexus-archiver/pull/121
