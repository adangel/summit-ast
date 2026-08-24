# Releasing Summit AST

A release is cut with the [maven-release-plugin][release-plugin]. The build itself never
uploads anything: the artifacts are signed with GPG and staged **locally** by
[njord][njord], and the resulting bundle ZIP is uploaded to the
[Maven Central Portal][portal] by hand. That last step is deliberately manual - no Maven
Central credentials are needed anywhere in the build.

## Prerequisites

* JDK 17 and the Maven wrapper (`./mvnw`). The enforcer requires Java `[17,18)` and
  Maven `[3.9.16,4.0.0)`.
* A GPG key whose public key is published to a keyserver, see
  [the Central documentation][gpg-docs].
* Push access to <https://github.com/adangel/summit-ast>. Note that this is the URL from
  `<developerConnection>` in `pom.xml`, which is where maven-release-plugin pushes -
  independent of how your git remotes happen to be named.
* An account on <https://central.sonatype.com> that is allowed to publish for the
  namespace `com.github.adangel`.
* Recommended, so that the njord goals can be called as `njord:<goal>` instead of
  `eu.maveniverse.maven.plugins:njord:<goal>` - add this to your `~/.m2/settings.xml`:

  ```xml
  <pluginGroups>
      <pluginGroup>eu.maveniverse.maven.plugins</pluginGroup>
  </pluginGroups>
  ```

### GPG passphrase and key

The maven-gpg-plugin runs with `<bestPractices>true</bestPractices>`, which means the
passphrase may only come from the environment variable `MAVEN_GPG_PASSPHRASE` or from
gpg-agent. Passing it as `-Dgpg.passphrase=...` or via `settings.xml` fails the build.

```bash
export MAVEN_GPG_PASSPHRASE=...
```

If gpg should not use its default key, set the fingerprint of the key to sign with
(`gpg --list-secret-keys` shows it):

```bash
export MAVEN_GPG_KEY_FINGERPRINT=0123456789ABCDEF0123456789ABCDEF01234567
```

This works through the `sign-key-from-env` profile in `pom.xml`: the gpg signer itself has
no environment variable for key selection, so the profile forwards the variable to the
plugin's `gpg.keyname` property. An environment variable is used rather than
`-Dgpg.keyname=...` because `release:perform` forks a new Maven process, which inherits the
environment but not the command line.

You can check that signing works before starting the release:

```bash
./mvnw clean verify -Psign
gpg --verify target/summit-ast-*-SNAPSHOT.jar.asc target/summit-ast-*-SNAPSHOT.jar
```

## 1. Pre-flight checks

```bash
./mvnw clean verify
./mvnw artifact:check-buildplan
./mvnw clean verify artifact:compare
```

The last two verify that the build is still reproducible. Run them without `-Psign`:
signatures are not reproducible and are not part of the comparison.

## 2. Prepare the release

```bash
./mvnw release:prepare
```

This asks for the release version, the tag name (`release/X.Y.Z` is prefilled) and the next
development version, runs `clean verify`, then commits and pushes both the release commit
and the next-development-version commit, plus the tag.

While it rewrites the versions, the release plugin also sets
`project.build.outputTimestamp`, the property that keeps the build reproducible, to the
time of the release. There is nothing to bump by hand.

Rehearse it first if you want to; `release:clean` removes the files it leaves behind:

```bash
./mvnw release:prepare -DdryRun=true
./mvnw release:clean
```

## 3. Build, sign and stage locally

```bash
./mvnw release:perform
```

This checks out the tag into `target/checkout` and builds it with the `sign` profile
active, so every artifact gets a detached `.asc` signature. The `deploy` at the end of that
build does not go to a remote repository: `<distributionManagement>` points at
`njord:template:release-sca`, so njord stages everything into a local store instead. The
`-sca` template adds SHA-512 and SHA-256 checksums on top of the default SHA-1 and MD5.

Stores live in njord's base directory (`~/.njord` by default, override with
`-Dnjord.basedir=...`); `./mvnw njord:status` prints the effective configuration.

## 4. Inspect and validate the staged store

```bash
./mvnw njord:list
```

```
[INFO] List of existing ArtifactStore:
[INFO] - summit-ast-00001 staged from com.github.adangel:summit-ast:jar:3.0.2 (..., RELEASE, release-sca, 8 artifacts)
```

The 8 artifacts are the pom, the main jar, the sources jar and the javadoc jar, plus one
`.asc` signature for each. Have a closer look and let njord check the store against the
rules of the Central Portal publisher:

```bash
./mvnw njord:list-content -Dnjord.store=summit-ast-00001
./mvnw njord:validate -Dnjord.details -Dnjord.store=summit-ast-00001
```

`-Dnjord.store=...` can be left out as long as there is only one store - njord then uses
the latest one staged by this project. The rules to validate against are those of the
Central Portal; they come from the `njord.publisher` property in `pom.xml`. Validating
does not contact the service.

## 5. Write the bundle ZIP

```bash
./mvnw njord:write-bundle -Dnjord.file=target/summit-ast-3.0.2-bundle.zip
```

The ZIP has Maven repository layout and contains the artifacts, their signatures and their
checksums. Use `-Dnjord.directory=<dir>` instead of `-Dnjord.file=<file>` to write
`<store name>.zip` into a directory.

## 6. Upload to Maven Central

Go to <https://central.sonatype.com/publishing>, choose *Publish Component* and upload the
bundle ZIP. Wait for the validation to finish, check the contents of the deployment, and
then release it. It takes a few minutes until the artifacts show up in Maven Central, and
somewhat longer until they are indexed by the search.

Nothing is published before you press the button, so a failed upload costs nothing: fix the
problem, drop the store, and stage again.

## 7. Clean up and finish

```bash
./mvnw njord:drop -Dnjord.store=summit-ast-00001
./mvnw release:clean
```

Then

* create a GitHub release for the `release/X.Y.Z` tag, and
* update the version in the usage snippet in [README.md](README.md).

## If something goes wrong

* **`release:prepare` fails or was interrupted** - `./mvnw release:rollback` restores the
  poms and reverts the commits. If the tag was already pushed, delete it locally and
  remotely (`git tag -d release/X.Y.Z`, `git push --delete <remote> release/X.Y.Z`).
  `release:prepare` resumes from `release.properties` by default; use
  `./mvnw release:clean` first if you want to start over from scratch.
* **`release:perform` fails** - it can simply be run again once the cause is fixed. Drop a
  half-finished store first (`./mvnw njord:drop -Dnjord.store=...`), otherwise the next
  attempt stages a second store and `njord:list` gets confusing.
* **Signing asks for a passphrase in a non-interactive shell** - either export
  `MAVEN_GPG_PASSPHRASE`, or prime gpg-agent beforehand by signing something manually.

[release-plugin]: https://maven.apache.org/maven-release/maven-release-plugin/
[njord]: https://maveniverse.eu/docs/njord/
[portal]: https://central.sonatype.com/
[gpg-docs]: https://central.sonatype.org/publish/requirements/gpg/
