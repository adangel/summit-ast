# Summit-AST

The Summit-AST library defines an abstract syntax tree (AST) data structure to
represent Salesforce
[Apex](https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_intro_what_is_apex.htm)
source code, and it provides the translation of a parse tree into its AST
representation.

This is not an official Google product.

Original repository (now archived): https://github.com/google/summit-ast

Maven coordinates (from version 3.0.0+):

```
<dependency>
    <groupId>com.github.adangel</groupId>
    <artifactId>summit-ast</artifactId>
    <version>3.1.0</version>
</dependency>
```

## Dependencies

This is built on top of the
[apex-parser](https://github.com/apex-dev-tools/apex-parser) Apex parser, which is a
compiled [ANTLR4](https://github.com/antlr/antlr4) grammar.

All dependencies are downloaded and managed through the build system.

## Build

This software is built using [Apache Maven](https://maven.apache.org/).

```
$ ./mvnw compile
$ ./mvnw test
```

See [docs/releasing.md](docs/releasing.md) for how a new release is created and published to
Maven Central.

## Running

The primary output is an in-memory AST data structure. The library is intended
to be integrated into other development tools.

There is a small `SummitTool` demonstration executable, which parses Apex source
files, builds the AST, and prints basic information. It can be executed by
running:

```
$ ./mvnw compile exec:java -Dexec.mainClass="com.google.summit.SummitTool" -Dexec.args="[-json] [files | directories ...]"
```

Any directories will be recursively walked. The tool attempts to compile any
files with the extension `.cls` or `.trigger`.

If the optional argument `-json` is given, then the AST is serialized additionally
as json into a file. The file name will be the original Apex source file with
the extension `.json` added.
