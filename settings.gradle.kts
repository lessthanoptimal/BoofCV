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
    "integration:boofcv-deepboof",

    // AGP configures fine without an SDK; only executing an android task needs one. The
    // root aggregate tasks skip it because it applies no boofcv convention plugin.
    "integration:boofcv-android"
)
