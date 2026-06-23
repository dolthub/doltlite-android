# DoltLite for Android

[DoltLite](https://github.com/dolthub/doltlite) for Android: SQLite with
Dolt-style version control — `dolt_commit`, `dolt_branch`, `dolt_merge`,
`dolt_diff` — packaged as an AAR. A thin [JNA](https://github.com/java-native-access/jna)
layer over a bundled `libdoltlite.so`; the `dolt_*` functions are reached
through SQL.

Android's framework SQLite can't be swapped, so it can't provide the `dolt_*`
functions — this binding vendors the engine (one `libdoltlite.so` per ABI:
arm64-v8a, armeabi-v7a, x86_64, x86).

## Install

```kotlin
dependencies {
    implementation("com.dolthub:doltlite-android:0.11.20")
}
```

## Usage

```kotlin
import com.dolthub.doltlite.Doltlite

Doltlite("${context.filesDir}/app.db").use { db ->   // or ":memory:"
    db.execute("CREATE TABLE notes(id INTEGER PRIMARY KEY, body TEXT)")
    db.execute("INSERT INTO notes(body) VALUES (?)", "first note")

    val hash = db.doltCommit("add first note")   // Dolt version-control commit
    println("committed $hash")

    for (row in db.query("SELECT commit_hash, message FROM dolt_log")) {
        println("${row[0]}  ${row[1]}")
    }
}
```

`execute`/`query` take positional bind values (`?`); the `dolt_*` functions and
virtual tables (`dolt_log`, `dolt_status`, `dolt_diff`, `dolt_branches`, ...) are
invoked through SQL. `doltCommit` is the version-control commit (distinct from a
SQL transaction commit).

## How it's built

Releases are tagged by the [dolthub/doltlite](https://github.com/dolthub/doltlite)
release workflow. On a tag, this repo's workflow checks out the matching doltlite
engine, cross-compiles `libdoltlite.so` for each ABI with the NDK, assembles the
AAR, and publishes to Maven Central.

## License

Apache-2.0. SQLite itself is public domain.
