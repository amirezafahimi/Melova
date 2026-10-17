pluginManagement {
    repositories {
        // Google Maven (dl.google.com) is blocked on some networks; Myket mirrors it.
        maven {
            name = "MyketGoogleMirror"
            url = uri("https://maven.myket.ir")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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
        // Google Maven (dl.google.com) is blocked on some networks; Myket mirrors it.
        maven {
            name = "MyketGoogleMirror"
            url = uri("https://maven.myket.ir")
        }
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
 