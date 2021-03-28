# Test ZIP Files for Unicode Path Extra Field

These files are used to test for proper use of Unicode Path extra fields ([zip specification §4.6.9](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)) when unarchiving zip archives.

Both contain three empty files, one without a Unicode Path extra field, one with a good extra field (CRC matches the header file name), one with a stale extra field (mismatched CRC). By using different values in the header file name and the Unicode path, it can be distinguished which one was used. A compliant unarchiver will use the names marked in bold.

File name in header | CRC                   | Unicode Path
--------------------|-----------------------|--------------------
**nameonly-name**   |
goodextra-name      | CRC("goodextra-name") | **goodextra-extra**
**badextra-name**   | CRC("bogus")          | badextra-extra

The difference between the two archives is whether the Language Encoding Flag (EFS) is set, which indicates that the header file names are already in UTF-8. The specification is not explicit about which one wins when both the flag is set and a Unicode Path extra field is present (it only says archivers shouldn’t do that). In practice, all unarchivers I have seen (including Apache Commons Compress used by Plexus-Archiver) ignore the extra field when the flag is set, which is why only _efsclear.zip_ is useful for testing.

The archives were created by the included _GenerateZips.java_ using Commons Compress.
