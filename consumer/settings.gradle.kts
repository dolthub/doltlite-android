import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    val candidate = providers.gradleProperty("doltliteCandidateRepository").orNull
    repositories {
        if (candidate != null) {
            exclusiveContent {
                forRepository {
                    maven { url = uri(candidate) }
                }
                filter { includeModule("com.dolthub", "doltlite-android") }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "doltlite-consumer"
