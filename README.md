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

```xml
<dependency>
  <groupId>org.codehaus.plexus</groupId>
  <artifactId>plexus-archiver</artifactId>
  <version>4.12.0</version>
</dependency>
```

Check the badge above for the current version.

```java
@Inject
@Named("zip")
Archiver archiver;

archiver.addDirectory(new File("src/main/resources"));
archiver.setDestFile(new File("target/out.zip"));
archiver.createArchive();
```

Components are JSR-330 beans, resolved by role hint (`zip`, `jar`, `tar.gz`, …). No Plexus container is involved — [Eclipse Sisu](https://www.eclipse.org/sisu/) replaced that years ago. The [site](https://codehaus-plexus.github.io/plexus-archiver/) lists every supported format and its hint.

## Requirements

Java 8 or later.

## Documentation

- [Project site](https://codehaus-plexus.github.io/plexus-archiver/) — supported formats, file mappers and selectors
- [Javadoc](https://javadoc.io/doc/org.codehaus.plexus/plexus-archiver)
- Release notes for current versions are on [GitHub releases](https://github.com/codehaus-plexus/plexus-archiver/releases); older ones are in [ReleaseNotes.md](ReleaseNotes.md)

## Contributing

See [CONTRIBUTING.md](https://github.com/codehaus-plexus/.github/blob/master/CONTRIBUTING.md). In short: `mvn verify` builds, and run `mvn spotless:apply` before pushing or CI will fail on formatting.

Please report security vulnerabilities privately — see [SECURITY.md](https://github.com/codehaus-plexus/.github/blob/master/SECURITY.md), not a public issue.
