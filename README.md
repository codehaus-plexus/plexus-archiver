Plexus-archiver
===============

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/codehaus-plexus/plexus-archiver.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-archiver.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.plexus/plexus-archiver)
[![GitHub CI](https://github.com/codehaus-plexus/plexus-archiver/actions/workflows/maven.yml/badge.svg)](https://github.com/codehaus-plexus/plexus-archiver/actions/workflows/maven.yml)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/codehaus/plexus/plexus-archiver/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/codehaus/plexus/plexus-archiver/README.md)

The current master is now at https://github.com/codehaus-plexus/plexus-archiver

## What is Plexus Archiver?

Plexus Archiver is a high-level Java API for creating and extracting archives (ZIP, JAR, TAR, etc.). It provides a simple, unified interface for working with various archive formats, abstracting away the low-level details of archive manipulation.

## Comparison to Apache Commons Compress

Plexus Archiver builds on top of [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) (since version 2.5) and provides additional capabilities:

### Apache Commons Compress

Commons Compress is a low-level library that provides:
- Direct access to archive formats and compression algorithms
- Fine-grained control over archive entries and their attributes
- Support for a wide range of archive formats (ZIP, TAR, AR, CPIO, etc.)
- Streaming API for memory-efficient processing

### Plexus Archiver

Plexus Archiver is a higher-level abstraction layer that adds:

**Simplified API**: Easy-to-use builder-style interface for common archiving tasks without dealing with low-level stream handling.

**Advanced Features**:
- File selectors and filtering capabilities
- Automatic handling of file permissions and attributes
- Built-in support for directory scanning with includes/excludes patterns
- Reproducible builds support (configurable timestamps and ordering)
- Duplicate handling strategies
- File mappers for transforming entry names during archiving/unarchiving
- Protection against ZIP bombs (configurable output size limits)

**Build Tool Integration**: Designed for integration with build tools like Maven, with support for:
- Modular JAR creation (Java 9+ modules)
- Manifest generation and customization
- Archive finalizers for post-processing

**Dependency Injection Ready**: Includes JSR-330 annotations for easy integration with dependency injection frameworks.

### When to Use Which?

**Use Apache Commons Compress when:**
- You need fine-grained control over archive format details
- You're working with streaming data or large archives
- You need to support specialized or uncommon archive formats
- Memory efficiency is critical

**Use Plexus Archiver when:**
- You want a simple, declarative API for common archiving tasks
- You're building a Maven plugin or similar build tool
- You need reproducible builds with consistent archive ordering
- You want built-in file filtering and selection capabilities
- You need to create modular JARs or other specialized Java archives

## Important Hint

Based on a hint of snyk.io security team they have found a possible security issue.
Furthermore they have offered an patch to prevent the possible security issue.
This patch has been integrated into the Release 3.6.0

## Release Notes

Current release notes are maintained on GitHub [releases](https://github.com/codehaus-plexus/plexus-archiver/releases)

You can find details about the historical releases in the [Release Notes](https://github.com/codehaus-plexus/plexus-archiver/blob/master/ReleaseNotes.md).

