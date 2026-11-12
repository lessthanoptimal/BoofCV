/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pluginManagement {
    // Must be read inside this block: pluginManagement is evaluated in isolation before
    // the rest of the script.
    val kotlinVersion: String by settings

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }

    // A `plugins {}` block cannot read constants from buildSrc, so plugin versions are
    // centralized here. The conventions' own plugins (gversion, errorprone, spotless) are
    // versioned in buildSrc/build.gradle.kts and must not be given a version anywhere else.
    plugins {
        id("com.android.library") version "8.9.0"
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Every repository the build needs, declared once. Modules may not add their own.
// google() is required by the android module.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven { url = uri("https://jitpack.io") } // Allows annotations past Java 8 to be used
    }
}

rootProject.name = "BoofCV"

include(
    "examples", "demonstrations", "applications",

    "main:boofcv-core",
    "main:boofcv-ip", "main:boofcv-io", "main:boofcv-feature", "main:boofcv-geo",
    "main:boofcv-sfm", "main:boofcv-reconstruction", "main:boofcv-recognition",
    "main:boofcv-simulation",
    "main:checks", "main:autocode", "main:boofcv-learning",
    "main:boofcv-ip-multiview", "main:boofcv-types", "main:boofcv-test",

    "integration:boofcv-all",
    "integration:boofcv-javacv", "integration:boofcv-WebcamCapture",
    "integration:boofcv-jcodec", "integration:boofcv-swing",
    "integration:boofcv-ffmpeg", "integration:boofcv-pdf", "integration:boofcv-kotlin",
    "integration:boofcv-deepboof"
)

// The android module is only included when an Android SDK is found. Without one, AGP still
// configures, but root tasks such as build, test and publishToMavenLocal fail with
// "SDK location not found". Override with -Pboofcv.android=true|false.
// The SDK is looked up in the same order AGP uses: sdk.dir in local.properties, ANDROID_HOME,
// then ANDROID_SDK_ROOT.
fun findAndroidSdk(): Pair<String, String>? {
    val localProperties = file("local.properties")
    if (localProperties.isFile) {
        val props = java.util.Properties()
        localProperties.inputStream().use { props.load(it) }
        props.getProperty("sdk.dir")?.let { return "sdk.dir in local.properties" to it }
    }
    for (name in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
        System.getenv(name)?.takeIf { it.isNotBlank() }?.let { return name to it }
    }
    return null
}

fun androidSkipReason(): String? {
    val (source, path) = findAndroidSdk()
        ?: return "no Android SDK found. Set ANDROID_HOME or sdk.dir in local.properties."
    val dir = file(path)
    if (!dir.isDirectory)
        return "$source=$path does not exist."
    if (!dir.resolve("platforms").isDirectory)
        return "$source=$path does not look like an Android SDK (no 'platforms' directory). " +
            "It should point to the SDK root, which contains platforms/ and build-tools/."
    return null
}

when (providers.gradleProperty("boofcv.android").orNull?.lowercase()) {
    "true" -> include("integration:boofcv-android")
    "false" -> {}
    else -> {
        val reason = androidSkipReason()
        if (reason == null) {
            include("integration:boofcv-android")
        } else {
            logger.warn("WARNING: Skipping integration:boofcv-android because $reason " +
                "Use -Pboofcv.android=false to silence this warning.")
        }
    }
}
