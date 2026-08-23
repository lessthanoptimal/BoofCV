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

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// While BoofCV itself targets Java 25, the build scripts are compiled with an older JDK
// so the build works on machines that do not have 25 installed.
kotlin { jvmToolchain(17) }

dependencies {
    implementation("com.peterabeles.gversion:com.peterabeles.gversion.gradle.plugin:1.11.0")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:4.0.1")
    implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:7.2.1")
    // NOTE: deliberately no kotlin-gradle-plugin here. No convention references Kotlin types,
    // and putting it on the buildSrc classpath breaks the android module: the Kotlin Android
    // plugin would load from buildSrc's classloader, which cannot see AGP, giving
    // "NoClassDefFoundError: com/android/build/gradle/BaseExtension".
    // Kotlin and AGP versions are pinned in settings.gradle.kts pluginManagement instead.
}
