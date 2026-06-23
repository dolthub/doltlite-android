import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "2.0.20"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

// Version comes from the release workflow: -PdoltliteVersion=x.y.z (from the tag).
val doltliteVersion = (findProperty("doltliteVersion") as String?) ?: "0.0.0"

android {
    namespace = "com.dolthub.doltlite"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    // libdoltlite.so per ABI is produced by scripts/build-libs.sh into
    // src/main/jniLibs and bundled into the AAR.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // JNA provides the FFI bindings to libdoltlite; the @aar artifact bundles
    // JNA's own native dispatcher for Android.
    api("net.java.dev.jna:jna:5.14.0@aar")
}

mavenPublishing {
    // Publish the release variant + sources to the Central Portal, signed.
    configure(AndroidSingleVariantLibrary("release", sourcesJar = true, publishJavadocJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates("com.dolthub", "doltlite-android", doltliteVersion)

    pom {
        name.set("DoltLite for Android")
        description.set("SQLite with Dolt version control (branches, commits, merge, diff) for Android.")
        url.set("https://github.com/dolthub/doltlite-android")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("dolthub")
                name.set("DoltHub, Inc.")
            }
        }
        scm {
            url.set("https://github.com/dolthub/doltlite-android")
            connection.set("scm:git:https://github.com/dolthub/doltlite-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/dolthub/doltlite-android.git")
        }
    }
}
