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
 * A published module that also ships a runnable fat jar: examples, demonstrations,
 * applications.
 *
 * Configure per module with:
 *     boofcvApp {
 *         title.set("BoofCV Examples Jar")
 *         mainClass.set("boofcv.examples.ExampleLauncherApp")
 *         jarName.set("examples.jar")
 *     }
 */
plugins {
    id("boofcv.libs-conventions")
}

interface BoofcvAppExtension {
    val title: Property<String>
    val mainClass: Property<String>
    val jarName: Property<String>
}

val app = extensions.create<BoofcvAppExtension>("boofcvApp")

// A runnable application is not part of the combined library artifacts.
extensions.getByType<BoofcvLibraryExtension>().includeInAggregates.set(false)

// Everything that gets unpacked into the fat jar.
val fatJarContents by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations["runtimeClasspath"])
}

tasks.register<Jar>("${project.name}Jar") {
    group = "build"
    description = "Builds a single runnable jar containing this module and its dependencies."

    manifest {
        attributes(
            "Implementation-Title" to app.title,
            "Implementation-Version" to project.version,
            "Main-Class" to app.mainClass
        )
    }

    archiveFileName.set(app.jarName)
    destinationDirectory.set(layout.projectDirectory)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets["main"].output)
    from(fatJarContents.elements.map { jars ->
        jars.filter { it.asFile.name.endsWith("jar") }.map { zipTree(it.asFile) }
    }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}
