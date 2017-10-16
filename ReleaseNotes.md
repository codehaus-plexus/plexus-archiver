Plexus Archiver and Plexus-IO combined release notes
========================================================================

Since archiver depends on a given version of IO this list is cumulative,
any version includes *all* changes below.

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

Plexus IO 2.6.1
---------------

### Improvement

 * Performance improvement affecting mac/linux users
   with lots of small files in large archives.

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

Plexus IO 2.5
-------------

 * Proper support for closeable on zip archives.
 * Removed zip supporting PlexusIoZipFileResourceCollection; which now exists in plexus-archiver. (Drop in replacement,
   just change/add jar file).

Plexus Archiver 2.9.1
---------------------

 * Wrap-up release with plexus-io-2.4.1

Plexus IO 2.4.1
---------------

### Bugs

 * PLXCOMP-279 - PlexusIoProxyResourceCollection does not provide Closeable iterator
 * Fixed PLXCOMP-280 - SimpleResourceAttributes has incorrect value for
   default file mode

Plexus Archiver 2.9
-------------------

### Improvements

 * PLXCOMP-276 - Reduce number of ways to create a PlexusIoResource

### Bugs

 * PLXCOMP-277 - Archiver unable to determine file equailty

Plexus IO 2.4
-------------

### Improvements

 * PLXCOMP-274 - Simplify use of proxies
 * PLXCOMP-275 - Avoid leaky abstractions
 * PLXCOMP-276 - Reduce number of ways to create a PlexusIoResource

Plexus Archiver 2.8.4
---------------------

### Bugs

 * PLXCOMP-273 - Normalize file separators for duplicate check

Plexus Archiver 2.8.3
---------------------

### Bugs

 * PLXCOMP-271 - Implicit created directories do not obey proper dirMode
 * PLXCOMP-272 - Overriding dirmode/filemode breaks symlinks

Plexus IO 2.3.5
---------------

### Bugs

 * PLXCOMP-278 - Symlink attribute was not preserved through merged/overridden attributes

Plexus Archiver 2.8.2
---------------------

### Bugs

 * PLXCOMP-266 - In-place filtering of streams give incorrect content length
   for tar files

Plexus IO 2.3.4
---------------

### Bugs

 * PLXCOMP-270 - Escaping algoritghm leaks through to system classloader
 * PLXCOMP-272 - Overriding dirmode/filemode breaks symlinks

Plexus IO 2.3.3
---------------

### Bugs

 * PLXCOMP-267 - StreamTransformers are consistently applied to all collections

Plexus Archiver 2.8.1
---------------------

### Improvements

 * PLXCOMP-268 - Add diagnostic archivers

Plexus IO 2.3.2
---------------

### Bugs

 * PLXCOMP-265 - Locale in shell influences "ls" parsing for screenscraper

Plexus IO 2.3.1
---------------

### Bugs

 * PLXCOMP-264 - Thread safety issue in streamconsumer

Plexus Archiver 2.8
-------------------

### New Features

 * PLXCOMP-263 - Support on-the fly stream filtering

### Improvements

 * PLXCOMP-255 - Removed dependency plexus-container-default:1.0-alpha-9-stable-1

### Bugs

 * PLXCOMP-262 - Directory symlinks in zip files are incorrect

Plexus IO 2.3
-------------

### New Features

 * PLXCOMP-261 - Make plexus io collections support on-the-fly filtering

### Improvements

 * PLXCOMP-260 - Make plexus io collections iterable

Plexus Archiver 2.7.1
---------------------

### Improvements

 * PLXCOMP-257 - Inconsistent buffering

### Bugs

 * PLXCOMP-256 - Several archivers leaks file handles

Plexus IO 2.2
-------------

### Bugs

 * PLXCOMP-251 - Date parsing in "ls" screenscraping has locale dependencies
 * PLXCOMP-254 - Fix File.separatorChar normalization when prefixes are used

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

Plexus IO 2.1.4
---------------

### Improvements

 * PLXCOMP-250 - Upgrade maven-enforcer-plugin to 1.3.1

### Bugs

 * PLXCOMP-107 - Fail to unzip archive, which contains file with name
   'How_can_I_annotate_a_part_in_the_AAM%3F.Help'

Plexus Archiver 2.6.3
---------------------

### Bugs

* PLXCOMP-233 - Plexus archiver can create tarfiles with empty uid and gid bytes
* PLXCOMP-247 - Bug with windows AND java5

Plexus IO 2.1.3
---------------

### Bugs

 * PLXCOMP-247 - Bug with windows AND java5

Plexus Archiver 2.6.2
---------------------

### Bugs

  * PLXCOMP-238 - CRC Failure if compress=false and file size <= 4 bytes
  * PLXCOMP-245 - Archives created on windows get zero permissions,
    creates malformed permissions on linux

Plexus IO 2.1.2
---------------

### Bugs

 * PLXCOMP-244 - Don't try to set attributes of symbolic links
 * PLXCOMP-245 - Archives created on windows get zero permissions,
   creates malformed permissions on linux

Plexus Archiver 2.6.1
---------------------

### Bugs

 * PLXCOMP-243 - Restore JDK1.5 compatibility

Plexus IO 2.1.1
---------------

### Bugs

 * PLXCOMP-243 - Restore JDK1.5 compatibility

Plexus Archiver 2.6
-------------------

### Bugs

 * PLXCOMP-113 - zip unarchiver doesn't support symlinks (and trivial to fix)

### Improvement

 * PLXCOMP-64 - add symlink support to tar unarchiver
 * PLXCOMP-117 - add symbolic links managment

Plexus IO 2.1
-------------

### Improvements

 * PLXCOMP-64 - add symlink support to tar unarchiver
 * PLXCOMP-117 - add symbolic links managment

### Bugs

 * PLXCOMP-113 - zip unarchiver doesn't support symlinks (and trivial to fix)
 * PLXCOMP-241 - ResourcesTest.compare test failure
 * PLXCOMP-248 - Use java7 setAttributes and ignore useJvmChmod flag when applicable

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

Plexus IO 2.0.12
----------------

### Bugs

 * PLXCOMP-249 - Add support for java7 chmod

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
[pr-26]: https://github.com/codehaus-plexus/plexus-archiver/issues/26
[pr-27]: https://github.com/codehaus-plexus/plexus-archiver/issues/27
[pr-41]: https://github.com/codehaus-plexus/plexus-archiver/pull/41
