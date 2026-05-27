# Git Internals

A Kotlin CLI tool that reads and interprets raw Git object files from a `.git` directory — built stage by stage as a [Hyperskill project](https://hyperskill.org/projects/110).

## What it does

Implements four Git plumbing commands against a real `.git` directory:

| Command | What it does |
|---------|-------------|
| `cat-file` | Reads a Git object (blob, commit, or tree) by its 40-char hash and prints it |
| `list-branches` | Lists all branches from `refs/heads/`, marking the current branch with `*` |
| `log` | Walks the parent chain of a branch and prints the commit log |
| `commit-tree` | Recursively reads a commit's tree and prints every file path |

## Stages

| Stage | Feature |
|-------|---------|
| 001 | Read blob objects |
| 002 | Read commit objects (single parent) |
| 003 | Read merge commits (multiple parents) |
| 004 | Read tree objects (binary SHA-1 parsing) |
| 005 | List branches (`refs/heads/` + `HEAD` detection) |
| 006 | Git log traversal with merged-commit labeling |
| 007 | Commit tree — recursive file path listing |

## Tech stack

- Kotlin 2.x
- Java 21
- Gradle (Groovy DSL)
- JUnit 4 + hs-test (Hyperskill test harness)
- JGit (integration test fixture builder)

## Project structure

```
task/
├── src/gitinternals/
│   ├── Main.kt                # CLI entry point — command dispatcher
│   ├── GitObject.kt           # Sealed domain model (Blob, Commit, Tree)
│   ├── GitObjectReader.kt     # zlib decompression + object type dispatch
│   ├── GitObjectFormatter.kt  # Exact-output formatter for all object types
│   ├── GitPathResolver.kt     # Hash → loose object file path
│   ├── CommitParser.kt        # Commit body text parser
│   ├── Contributor.kt         # Author/committer identity + timestamp
│   ├── TreeParser.kt          # Binary tree body parser
│   ├── TreeEntry.kt           # Single tree entry (permissions, sha1, name)
│   ├── BranchLister.kt        # Branch listing from refs/heads/
│   ├── GitLogReader.kt        # Parent-chain traversal for git log
│   └── FullTreeReader.kt      # Recursive commit → tree → file paths
└── test/
    ├── GitInternalsTest.java              # Hyperskill E2E harness
    └── gitinternals/
        ├── integration/
        │   ├── CliIntegrationTest.kt
        │   ├── GitInternalsIntegrationTest.kt
        │   └── TreeIntegrationTest.kt
        └── unit/
            ├── BranchListerTest.kt
            ├── CommitParserTest.kt
            ├── FullTreeReaderTest.kt
            ├── GitLogReaderTest.kt
            ├── GitObjectFormatterTest.kt
            ├── GitObjectFormatterTreeTest.kt
            ├── GitObjectReaderTest.kt
            ├── GitObjectReaderTreeTest.kt
            ├── GitPathResolverTest.kt
            └── TreeParserTest.kt
```

## Running tests

```bash
./gradlew :Git_Internals-task:test
```

Full clean run:

```bash
./gradlew clean test
```

## Key design decisions

**Byte-level tree parsing** — Git tree objects embed 20-byte binary SHA-1 hashes. `TreeParser` accepts `ByteArray` and converts each byte manually to lowercase zero-padded hex, avoiding UTF-8 decoding corruption.

**Sealed `GitObject` hierarchy** — `Blob`, `Commit`, and `Tree` share a sealed class. Every `when` dispatch is exhaustive at compile time, so adding a new type forces all dispatch sites to be updated.

**No trailing newlines** — The Hyperskill checker compares output line-by-line. All formatters use `append` instead of `appendLine` for the last line to avoid spurious blank lines.

**Injectable dependencies** — `GitObjectReader`, `GitLogReader`, and `FullTreeReader` accept collaborators as constructor parameters with defaults, enabling unit tests without mocking frameworks.
