# Plexus Archiver

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-archiver.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-archiver)
[![GitHub CI](https://github.com/codehaus-plexus/plexus-archiver/actions/workflows/maven.yml/badge.svg)](https://github.com/codehaus-plexus/plexus-archiver/actions/workflows/maven.yml)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/codehaus/plexus/plexus-archiver/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/codehaus/plexus/plexus-archiver/README.md)
[![License](https://img.shields.io/github/license/codehaus-plexus/plexus-archiver.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)

One API for creating and extracting archives — zip, jar, tar and their compressed variants — regardless of format. It is what `maven-assembly-plugin`, `maven-jar-plugin` and friends use underneath.

Built on [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/), adding the things a build tool needs on top of it: include/exclude scanning, file mappers and selectors, permission handling, reproducible output, duplicate strategies, modular JARs and archive size limits.

## Status

Actively maintained. This is one of the most widely used artifacts in the Maven ecosystem, so public API is kept compatible and changes are deliberately conservative.

## Using it

**pom.xml** — the `5.x` line, which requires Java 17:

```xml
<dependency>
  <groupId>org.codehaus.plexus</groupId>
  <artifactId>plexus-archiver</artifactId>
</dependency>
```

The `4.x` line, still maintained, for Java 8:

```xml
<dependency>
  <groupId>org.codehaus.plexus</groupId>
  <artifactId>plexus-archiver</artifactId>
</dependency>
```

Take the current version of each line from the [releases page](https://github.com/codehaus-plexus/plexus-archiver/releases); the badge above tracks whichever is newest overall.

**Java sourcecode**

Components are JSR-330 beans, resolved by role hint (`zip`, `jar`, `tar.gz`, …). No Plexus container is involved — [Eclipse Sisu](https://www.eclipse.org/sisu/) replaced that years ago. The [site](https://codehaus-plexus.github.io/plexus-archiver/) lists every supported format and its hint.

```java
@Inject
@Named("zip")
Archiver archiver;

@Inject
ArchiverManager archiverManager;
```

As of version 5.0.0 there's also a [ServiceLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html) based implementation.

```java
var archiveManager = new ServiceLoaderArchiverManager();

// or without archiveManager
var archiver = ServiceLoader.load(ArchiverProvider.class).stream()
  .map(Provider::get)
  .filter(p -> "zip".equals(p.getName()))
  .findFirst()
  .orElseThrow();
```

After retrieving a fresh archiver, you can do

```java
var archiver = archiveManager.getArchiver("zip");

archiver.addDirectory(new File("src/main/resources"));
archiver.setDestFile(new File("target/out.zip"));
archiver.createArchive();
```

## Documentation

- [Project site](https://codehaus-plexus.github.io/plexus-archiver/) — supported formats, file mappers and selectors
- [Javadoc](https://javadoc.io/doc/org.codehaus.plexus/plexus-archiver)
- Release notes for current versions are on [GitHub releases](https://github.com/codehaus-plexus/plexus-archiver/releases); older ones are in [ReleaseNotes.md](ReleaseNotes.md)

## Contributing

See [CONTRIBUTING.md](https://github.com/codehaus-plexus/.github/blob/master/CONTRIBUTING.md). In short: `mvn verify` builds, and run `mvn spotless:apply` before pushing or CI will fail on formatting.

Please report security vulnerabilities privately — see [SECURITY.md](https://github.com/codehaus-plexus/.github/blob/master/SECURITY.md), not a public issue.
