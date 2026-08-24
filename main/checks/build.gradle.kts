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
	id("boofcv.java-conventions")
}

// Every core module, found by path rather than by a hand-maintained list.
val coreModules = rootProject.subprojects.filter { it.path.startsWith(":main:boofcv-") }

// Native binaries for this machine. The published ones are optional, so anything that
// actually decodes video has to ask for them explicitly.
val hostPlatform = rootProject.extra["hostPlatform"] as String

dependencies {
	api(project(":main:boofcv-core"))
	api(project(":main:autocode"))

	// The regression runner executes the other modules' actual tests and benchmarks, so it
	// needs their compiled output, not just the shared fixtures in boofcv-test. These come
	// through the consumable configurations declared in boofcv.java-conventions.
	coreModules.forEach {
		testImplementation(project(path = it.path, configuration = "testOutput"))
		runtimeOnly(project(path = it.path, configuration = "benchmarkOutput"))
	}
	testImplementation(project(":integration:boofcv-swing"))
	runtimeOnly(project(path = ":main:boofcv-feature", configuration = "experimentalOutput"))
	runtimeOnly(project(":integration:boofcv-ffmpeg")) // one test decodes a mp4

	implementation(Libs.REGRESSION) { exclude(group = "org.yaml") }
	implementation(Libs.LANGUAGE)
	api(Libs.JMH_CORE)
	runtimeOnly("${Libs.FFMPEG}:$hostPlatform")
}

// checks is a test harness. It applies only boofcv.java-conventions, so there is no
// publish task; only the useless empty jar needs turning off.
tasks.named("jar") { enabled = false }

// Run the regression using a gradle command. Currently this is the only way to get paths
// set up for benchmarks.
//
//   ./gradlew runtimeRegression --console=plain --args="--SummaryOnly"
tasks.register<JavaExec>("runtimeRegression") {
	dependsOn("build")
	group = "execution"
	description = "Runs the JMH runtime regression (boofcv.regression.BoofCVRuntimeRegressionApp)"
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("boofcv.regression.BoofCVRuntimeRegressionApp")
	// Arguments come from Gradle's built-in --args option, which handles quoting/splitting.
}
