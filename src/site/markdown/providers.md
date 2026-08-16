---
title: Service Providers
---

# Service Providers

Since 5.0.0, Plexus Archiver exposes Java `ServiceLoader` providers for archive creation, extraction, and resource
collections. Providers create a fresh instance, apply its initial configuration, and only then return it to the caller.
There is no public no-argument creation method.

| Provider | Configuration |
|:---|:---|
| [`ArchiverProvider`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/ArchiverProvider.html) | [`ArchiverConfigurer`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/ArchiverConfigurer.html) |
| [`UnArchiverProvider`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/UnArchiverProvider.html) | [`UnArchiverConfigurer`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/UnArchiverConfigurer.html) |
| [`PlexusIoResourceCollectionProvider`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/PlexusIoResourceCollectionProvider.html) | [`PlexusIoResourceCollectionConfigurer`](./apidocs/index.html?org/codehaus/plexus/archivers/spi/PlexusIoResourceCollectionConfigurer.html) |

Providers use the same format names listed on the [introduction](index.html). Resolve one with `ServiceLoader` and
select it by `getName()`.

Applications that already use `ArchiverManager` can apply the same configuration without resolving a provider
directly:

```java
Archiver archiver = manager.getArchiverFactory("zip").create(configurer -> {
    configurer.setDestFile(Path.of("target/archive.zip"));
    configurer.addFileSet(FileSet.of(Path.of("src/main/resources")));
});

UnArchiver unarchiver = manager.getUnArchiverFactory(Path.of("archive.zip").toFile()).create(configurer -> {
    configurer.setSource(Path.of("archive.zip"));
    configurer.setDestinationDirectory(Path.of("target/unpacked"));
});
```

Factory lookups are available by format name and by file for archivers, unarchivers, and resource collections. A
resolved factory can create multiple independently configured instances. The existing object-returning manager methods
remain unchanged and are equivalent to creating from the corresponding factory with an empty configuration.

## Creating an archive

```java
ArchiverProvider zip = ServiceLoader.load(ArchiverProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .filter(provider -> provider.getName().equals("zip"))
        .findFirst()
        .orElseThrow();

Archiver archiver = zip.newArchiver(configurer -> {
    configurer.setDestFile(Path.of("target/archive.zip"));
    configurer.addFileSet(FileSet.of(Path.of("src/main/resources"))
            .prefixed("resources/")
            .excluding(List.of("**/*.tmp")));
    configurer.setDuplicateBehavior(DuplicateHandling.FAIL);
});

archiver.createArchive();
```

`ArchiverConfigurer` uses `Path`, sealed policy types, and `UnixPermissions` rather than exposing legacy `File`,
boolean, string, or octal-mode encodings.

## Extracting an archive

```java
UnArchiverProvider zip = ServiceLoader.load(UnArchiverProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .filter(provider -> provider.getName().equals("zip"))
        .findFirst()
        .orElseThrow();

UnArchiver unarchiver = zip.newUnarchiver(configurer -> {
    configurer.setSource(Path.of("target/archive.zip"));
    configurer.setDestinationDirectory(Path.of("target/unpacked"));
    configurer.setExistingFileHandling(ExistingFileHandling.KEEP_NEWER);
    configurer.setPermissionHandling(PermissionHandling.PRESERVE);
});

unarchiver.extract();
```

## Reading a resource collection

```java
PlexusIoResourceCollectionProvider zip = ServiceLoader.load(PlexusIoResourceCollectionProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .filter(provider -> provider.getName().equals("zip"))
        .findFirst()
        .orElseThrow();

PlexusIoResourceCollection resources = zip.newPlexusIoResourceCollection(configurer -> {
    configurer.setSource(Path.of("target/archive.zip"));
    configurer.setPrefix("dependencies/");
    configurer.setIncludes(List.of("**/*.class"));
    configurer.setEncoding(StandardCharsets.UTF_8);
});
```

Common collection settings include prefixes, include/exclude patterns, selectors, mappers, stream transformers, case
sensitivity, default excludes, and empty-directory handling. Source and encoding settings are capability-aware: an
unsupported setting fails during configuration rather than during resource iteration.

## Implementing a provider

The provider interfaces are sealed and each permits one non-sealed abstract base implementation. Third-party providers
extend the matching base class and implement the protected creation hook:

```java
public final class ExampleUnArchiverProvider extends AbstractUnArchiverProvider {
    @Override
    public String getName() {
        return "example";
    }

    @Override
    protected UnArchiver createUnarchiver() {
        return new ExampleUnArchiver();
    }
}
```

Register the concrete provider using the normal `ServiceLoader` mechanism. The base class owns configured creation and
keeps the raw instance creation hook unavailable to consumers.