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

// Applies none of the boofcv.* convention plugins: AGP cannot coexist with java-library.
// That is also what keeps this module out of the root aggregate tasks, which select by
// convention plugin.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

android {
    namespace = "${project.group}.android.library"
    compileSdk = 33

    defaultConfig {
        minSdk = 22 // Depends on which API you use
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

configurations.configureEach {
    resolutionStrategy { force(Libs.JETBRAINS_ANNOTATIONS) }
}

dependencies {
    api(project(":main:boofcv-ip"))
    api(project(":main:boofcv-feature"))
    api(project(":main:boofcv-geo"))

    val fragmentVersion = "2.7.7"

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:$fragmentVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$fragmentVersion")
    implementation("androidx.annotation:annotation:1.7.1")

    compileOnly(Libs.JETBRAINS_ANNOTATIONS) // @Nullable

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.0")

    annotationProcessor(Libs.JABEL)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "${project.group}"
            artifactId = project.name
            version = "${project.version}"
            boofcvPom()

            afterEvaluate { from(components["release"]) }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://central.sonatype.com/api/v1/publish/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            )
            credentials {
                username = (project.findProperty("ossrhUsername") as String?) ?: "dummy"
                password = (project.findProperty("ossrhPassword") as String?) ?: "dummy"
            }
        }
    }
}

// Only sign when real credentials are present. Test the VALUE, not whether the property
// exists: it is defaulted to "dummy" below, so an existence check is always true and
// signing would run with no key.
val ossrhPassword = project.findProperty("ossrhPassword") as String?
if (ossrhPassword != null && ossrhPassword != "dummy") {
    signing { sign(publishing.publications["release"]) }
}
