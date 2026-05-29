pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        //maven(url = "https://maven.myket.ir")
        maven {
            name = "TarsosDSP"
            url = uri("https://mvn.0110.be/releases")
            content {
                includeGroup("be.tarsos.dsp")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //maven(url = "https://maven.myket.ir")
        google()
        mavenCentral()
        maven {
            name = "TarsosDSP"
            url = uri("https://mvn.0110.be/releases")
            content {
                includeGroup("be.tarsos.dsp")
            }
        }
    }
}

rootProject.name = "Melova"
include(":app")
 