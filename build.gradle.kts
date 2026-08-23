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
    id("io.github.gradle-nexus.publish-plugin")
}

// Which native platforms are supported can be specified on the command line. Otherwise the
// default is to support all of them. Read by boofcv-ffmpeg and boofcv-javacv.
val nativeArch: List<String> by extra(
    if (project.hasProperty("native_arch")) listOf(project.property("native_arch") as String)
    else listOf("linux-x86_64", "macosx-x86_64", "windows-x86_64")
)

// Sonatype publishing coordination. Only configured when credentials are present; otherwise
// the relevant tasks are unusable but the build still works.
if (project.hasProperty("ossrhUsername")) {
    nexusPublishing {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
                username.set(project.property("ossrhUsername") as String)
                password.set(project.property("ossrhPassword") as String)
            }
        }
    }
}

// Modules are discovered by which convention plugin they apply, not by a list of names.
// The android module applies neither convention, so it is never in these lists.
val publishedModules: List<Project>
    get() = subprojects.filter { it.plugins.hasPlugin("boofcv.libs-conventions") }

// The modules whose own code belongs in the combined artifacts. Excludes aggregator POMs
// and the runnable apps; each module declares this for itself via `boofcvLibrary`.
val aggregateModules: List<Project>
    get() = publishedModules.filter {
        it.extensions.getByType<BoofcvLibraryExtension>().includeInAggregates.get()
    }

val javaModules: List<Project>
    get() = subprojects.filter { it.plugins.hasPlugin("boofcv.java-conventions") }

fun Project.mainSourceSet(): SourceSet = extensions.getByType<SourceSetContainer>()["main"]

// Every library module plus its transitive external dependencies. Declared on the root
// project because a task may not resolve another project's configuration.
val libraryRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// Same idea, but for javadoc: it also needs the compile-only annotation libraries that
// runtimeClasspath does not carry.
val libraryJavadoc: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(libraryRuntime)
}

dependencies {
    libraryJavadoc(Libs.LOMBOK)
    libraryJavadoc(Libs.JETBRAINS_ANNOTATIONS)
    libraryJavadoc(Libs.JSR250)
}

gradle.projectsEvaluated {
    aggregateModules.forEach { dependencies.add(libraryRuntime.name, it) }
}

// Creates a directory with all the compiled BoofCV jars and the dependencies for main
tasks.register("createLibraryDirectory") {
    group = "build"
    description = "Collects every library jar, its sources jar, and all external dependencies."
    publishedModules.forEach { evaluationDependsOn(it.path) }

    val outputDir = layout.projectDirectory.dir("boofcv-v$version-libs").asFile
    val sourceJars = aggregateModules.map { it.tasks.named<Jar>("sourcesJar") }
    dependsOn(sourceJars)

    val runtime = libraryRuntime

    doLast {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        copy {
            from(runtime.filter { !it.isDirectory }) // module jars + external dependencies
            from(sourceJars.map { it.get().archiveFile })
            into(outputDir)
        }
        println("\n\nSaved to directory ${outputDir.name}")
    }
}

// Creates a single jar which contains all the published modules
tasks.register<Jar>("oneJarBin") {
    group = "build"
    description = "Single jar containing the compiled output of every published module."
    publishedModules.forEach { evaluationDependsOn(it.path) }
    dependsOn(aggregateModules.map { it.tasks.named("compileJava") })

    archiveFileName.set("boofcv-v$version.jar")
    destinationDirectory.set(layout.projectDirectory)

    from(aggregateModules.map { it.mainSourceSet().output }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}

// Generates a global javadoc from all the modules
tasks.register<Javadoc>("alljavadoc") {
    group = "documentation"
    publishedModules.forEach { evaluationDependsOn(it.path) }
    dependsOn(aggregateModules.map { it.tasks.named("compileJava") })

    // Only include source in src/ to avoid pulling in third party code some modules vendor
    source(aggregateModules.map { it.mainSourceSet().allJava })
    classpath = libraryJavadoc

    setDestinationDir(file("docs/api"))

    (options as StandardJavadocDocletOptions).apply {
        // Hack for Java 8u121 and beyond
        addBooleanOption("-allow-script-in-comments", true)
        addBooleanOption("html5", true)
        // Add a list of uses of a class to javadoc
        use(true)
        isFailOnError = false
        docTitle = "BoofCV ($version)"
        links(
            "https://docs.oracle.com/en/java/javase/11/docs/api",
            "https://ejml.org/javadoc/",
            "https://georegression.org/javadoc/",
            "https://ddogleg.org/javadoc/"
        )
    }

    // Gradle does not copy doc-files
    doLast {
        copy {
            from(aggregateModules.map { it.fileTree("src/main/java").include("**/doc-files/*") })
            into(destinationDir!!)
        }
    }
}

// Same javadoc, but written to docs/api-web and carrying the analytics footer
tasks.register("alljavadocWeb") {
    group = "documentation"
    description = "alljavadoc for the website: writes to docs/api-web and adds misc/bottom.txt."
    doFirst {
        val javadoc = tasks.named<Javadoc>("alljavadoc").get()
        (javadoc.options as StandardJavadocDocletOptions).bottom = file("misc/bottom.txt").readText()
        javadoc.setDestinationDir(file("docs/api-web"))
    }
    finalizedBy(tasks.named("alljavadoc"))
}

tasks.register<TestReport>("testReport") {
    group = "verification"
    description = "Aggregates the test results of every Java module into one report."
    destinationDirectory.set(layout.buildDirectory.dir("reports/allTests"))
    javaModules.forEach { evaluationDependsOn(it.path) }
    testResults.from(javaModules.map { it.tasks.named<Test>("test") })
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
    gradleVersion = "9.2.0"
}
