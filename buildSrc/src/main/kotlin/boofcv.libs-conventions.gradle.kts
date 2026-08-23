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

/**
 * A published BoofCV module. Applying this is what puts a module into the released
 * artifacts, the aggregate javadoc, and oneJarBin.
 */
plugins {
    id("boofcv.java-conventions")
    `maven-publish`
    signing
}

val boofcvLibrary = extensions.create<BoofcvLibraryExtension>("boofcvLibrary")
boofcvLibrary.includeInAggregates.convention(true)

// Force the release build to fail if it depends on a SNAPSHOT
tasks.named("jar") { dependsOn("checkDependsOnSNAPSHOT") }

// Force publish to fail if trying to upload a stable release and git is dirty
tasks.named("publish") { dependsOn("failDirtyNotSnapshot") }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            boofcvPom()
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/"
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
// exists: it is defaulted to "dummy" above, so an existence check is always true and
// signing would run with no key.
val ossrhPassword = project.findProperty("ossrhPassword") as String?
if (ossrhPassword != null && ossrhPassword != "dummy") {
    signing { sign(publishing.publications["mavenJava"]) }
}
