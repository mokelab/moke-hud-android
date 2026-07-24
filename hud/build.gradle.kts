plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.mokelab.hud.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            // Robolectric が host-side でリソース/フレームワークを参照できるようにする。
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

group = "com.mokelab.hud"
version = "0.1.1"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "hud-android"
                pom {
                    name.set("MokeHud Android")
                    description.set(
                        "Debug-time HUD overlay for Analytics events on Android",
                    )
                    url.set("https://github.com/mokelab/moke-hud-android")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set(
                                "https://www.apache.org/licenses/LICENSE-2.0.txt",
                            )
                            distribution.set("repo")
                        }
                    }
                    scm {
                        url.set("https://github.com/mokelab/moke-hud-android")
                        connection.set(
                            "scm:git:https://github.com/mokelab/moke-hud-android.git",
                        )
                    }
                }
            }
        }
        repositories {
            maven {
                name = "githubPages"
                url = uri(rootProject.layout.projectDirectory.dir(".gh-pages"))
            }
        }
    }
}
