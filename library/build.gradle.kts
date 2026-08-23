import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "eu.pascap"
version = System.getenv("RELEASE_VERSION") ?: "1.0.0"

kotlin {
    jvm()
    android {
        namespace = "eu.pascap.kotlinxIoTar"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kotlinIoTar", version.toString())

    pom {
        name = "kotlinx-io-tar"
        description = "Library that allows to read/write tar files with kotlinx-io"
        inceptionYear = "2026"
        url = "https://github.com/Pascap-LTD/kotlinx-io-tar/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "pascap-ltd"
                name = "Pascap LTD"
                url = "https://pascap.eu"
            }
        }
        scm {
            url = "https://github.com/Pascap-LTD/kotlinx-io-tar/"
            connection = "scm:git:git://github.com/Pascap-LTD/kotlinx-io-tar.git"
            developerConnection = "scm:git:ssh://git@github.com/Pascap-LTD/kotlinx-io-tar.git"
        }
    }
}
