plugins {
    id("com.android.application") version "8.5.2"
}

android {
    namespace = "com.dolthub.doltlite.smoke"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dolthub.doltlite.smoke"
        minSdk = 21
        targetSdk = 34
        testInstrumentationRunner = "com.dolthub.doltlite.smoke.SmokeRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.dolthub:doltlite-android:${providers.gradleProperty("doltliteVersion").get()}")
}
