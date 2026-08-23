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
	id("boofcv.libs-conventions")
}

// A dependency aggregator: it exists to pull in the other modules, so its own output does
// not belong in oneJarBin or alljavadoc.
boofcvLibrary { includeInAggregates.set(false) }

dependencies {
	api(project(":main:boofcv-feature"))
	api(project(":main:boofcv-geo"))
	api(project(":main:boofcv-io"))
	api(project(":main:boofcv-ip"))
	api(project(":main:boofcv-ip-multiview"))
	api(project(":main:boofcv-learning"))
	api(project(":main:boofcv-recognition"))
	api(project(":main:boofcv-reconstruction"))
	api(project(":main:boofcv-sfm"))
	api(project(":main:boofcv-types"))
}

// Print out information about dependencies to stdout. Intended use is so we know exactly
// what CI is running.
tasks.register<JavaExec>("dependencyInfo") {
	mainClass.set("boofcv.PrintDependenciesVersionInfo")
	classpath = sourceSets["main"].runtimeClasspath
}
