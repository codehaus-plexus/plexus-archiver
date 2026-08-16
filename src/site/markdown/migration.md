# Migration Guide

This page documents breaking API changes by major version and their replacements.

---

## Migrating to 5.0.0

The following APIs that were previously marked `@Deprecated` have been removed in 5.0.0.

### `Archiver`: `addDirectory` overloads → `addFileSet`

The four `addDirectory` overloads on `Archiver` are removed. Use `addFileSet` with a `DefaultFileSet` instead.

| Removed | Replacement |
|---------|-------------|
| `addDirectory(File dir)` | `addFileSet(DefaultFileSet.fileSet(dir))` |
| `addDirectory(File dir, String prefix)` | `addFileSet(DefaultFileSet.fileSet(dir).prefixed(prefix))` |
| `addDirectory(File dir, String[] includes, String[] excludes)` | `addFileSet(DefaultFileSet.fileSet(dir).includeExclude(includes, excludes))` |
| `addDirectory(File dir, String prefix, String[] includes, String[] excludes)` | `addFileSet(DefaultFileSet.fileSet(dir).prefixed(prefix).includeExclude(includes, excludes))` |

```java
// Before
archiver.addDirectory(new File("src/main/resources"));
archiver.addDirectory(new File("src/main/resources"), "resources/");
archiver.addDirectory(new File("src"), null, new String[]{"**/*.class"});

// After
import org.codehaus.plexus.archiver.util.DefaultFileSet;

archiver.addFileSet(DefaultFileSet.fileSet(new File("src/main/resources")));
archiver.addFileSet(DefaultFileSet.fileSet(new File("src/main/resources")).prefixed("resources/"));
archiver.addFileSet(DefaultFileSet.fileSet(new File("src")).includeExclude(null, new String[]{"**/*.class"}));
```

---

### `Archiver`: `addArchivedFileSet(File)` overloads → `addArchivedFileSet(ArchivedFileSet)`

The four `addArchivedFileSet(File, ...)` overloads are removed. Use `addArchivedFileSet` with a `DefaultArchivedFileSet` instead.

| Removed | Replacement |
|---------|-------------|
| `addArchivedFileSet(File archive)` | `addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(archive))` |
| `addArchivedFileSet(File archive, String prefix)` | `addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(archive).prefixed(prefix))` |
| `addArchivedFileSet(File archive, String[] includes, String[] excludes)` | `addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(archive).includeExclude(includes, excludes))` |
| `addArchivedFileSet(File archive, String prefix, String[] includes, String[] excludes)` | `addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(archive).prefixed(prefix).includeExclude(includes, excludes))` |

```java
// Before
archiver.addArchivedFileSet(new File("libs/dependency.jar"));
archiver.addArchivedFileSet(new File("libs/dependency.jar"), "lib/");

// After
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;

archiver.addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(new File("libs/dependency.jar")));
archiver.addArchivedFileSet(DefaultArchivedFileSet.archivedFileSet(new File("libs/dependency.jar")).prefixed("lib/"));
```

---

### `Archiver`: `getFiles()` → `getResources()`

`getFiles()` returned a `Map<String, ArchiveEntry>` snapshot; `getResources()` returns a lazy `ResourceIterator`.

```java
// Before
Map<String, ArchiveEntry> files = archiver.getFiles();
for (Map.Entry<String, ArchiveEntry> e : files.entrySet()) {
    process(e.getKey(), e.getValue());
}

// After
ResourceIterator it = archiver.getResources();
while (it.hasNext()) {
    ArchiveEntry entry = it.next();
    process(entry.getName(), entry);
}
```

---

### `Archiver` / `UnArchiver`: `setUseJvmChmod` / `isUseJvmChmod` — removed with no replacement

These methods were no-ops since version 2.2: the JVM chmod path has always been used. Simply remove any calls to them.

```java
// Before
archiver.setUseJvmChmod(true);   // remove this line
unArchiver.setUseJvmChmod(true); // remove this line
```

---

### `Archiver`: `setLastModifiedDate(Date)` / `getLastModifiedDate()` → `FileTime` API

`java.util.Date`-based methods are replaced by `java.nio.file.attribute.FileTime`.

| Removed | Replacement |
|---------|-------------|
| `setLastModifiedDate(Date date)` | `setLastModifiedTime(FileTime.fromMillis(date.getTime()))` |
| `getLastModifiedDate()` | `getLastModifiedTime()` (returns `FileTime`) |

```java
// Before
archiver.setLastModifiedDate(new Date(1234567890000L));
Date d = archiver.getLastModifiedDate();

// After
import java.nio.file.attribute.FileTime;

archiver.setLastModifiedTime(FileTime.fromMillis(1234567890000L));
FileTime ft = archiver.getLastModifiedTime();
```

---

### `Archiver`: `configureReproducible(Date)` → `configureReproducibleBuild(FileTime)`

```java
// Before
archiver.configureReproducible(new Date(1234567890000L));

// After
import java.nio.file.attribute.FileTime;

archiver.configureReproducibleBuild(FileTime.fromMillis(1234567890000L));
```

---

### `JarArchiver`: JAR Index support removed

`setIndex(boolean)`, `addConfiguredIndexJars(File)`, and `setManifestEncoding(String)` are removed.
JAR Index was deprecated by the JDK itself ([JDK-8302819](https://bugs.openjdk.org/browse/JDK-8302819)) and is no longer supported by modern class loaders.

Simply remove any calls to these methods. No functional replacement is needed — modern JVMs do not use the index for class loading.

```java
// Before
archiver.setIndex(true);
archiver.addConfiguredIndexJars(dependencyJar);
archiver.setManifestEncoding("UTF-8"); // was already a no-op

// After — remove all three lines
```

---

### `ArchiveFileFilter` / `JarSecurityFileFilter` / `FilterSupport` — removed

These classes are removed. Use the `FileSelector` API via `addFileSet` instead.

| Removed | Replacement |
|---------|-------------|
| `ArchiveFileFilter` | `org.codehaus.plexus.components.io.fileselectors.FileSelector` |
| `JarSecurityFileFilter` | `org.codehaus.plexus.archiver.filters.JarSecurityFileSelector` |
| `FilterSupport` | `FileSelector` passed directly to a `FileSet` |

```java
// Before
archiver.addArchivedFileSet(archive, new ArchiveFileFilter[]{new JarSecurityFileFilter()});

// After
import org.codehaus.plexus.archiver.filters.JarSecurityFileSelector;
import org.codehaus.plexus.archiver.util.DefaultArchivedFileSet;

DefaultArchivedFileSet fileSet = DefaultArchivedFileSet.archivedFileSet(archive);
fileSet.setFileSelectors(new FileSelector[]{new JarSecurityFileSelector()});
archiver.addArchivedFileSet(fileSet);
```

---

### `AbstractArchiver`: `getRawDefaultFileMode()` → `getDefaultFileMode()`

```java
// Before
int mode = archiver.getRawDefaultFileMode();

// After
int mode = archiver.getDefaultFileMode();
```

---

### `ArchiveEntry`: `getFile()` → `getInputStream()`

`ArchiveEntry` entries are no longer backed by `File` objects. Use the resource stream instead.

```java
// Before
File f = entry.getFile();

// After
InputStream in = entry.getInputStream();
```

---

### `zip/AddedDirs`: `asStringStack()` → `asStringDeque()`

```java
// Before
Stack<String> dirs = addedDirs.asStringStack(entry);

// After
Deque<String> dirs = addedDirs.asStringDeque(entry);
```
