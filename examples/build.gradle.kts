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

import net.ltgt.gradle.errorprone.errorprone

plugins {
	id("boofcv.app-conventions")
}

boofcvApp {
	title.set("BoofCV Examples Jar")
	mainClass.set("boofcv.examples.ExampleLauncherApp")
	jarName.set("examples.jar")
}

// Native binaries for this machine. The published ones are optional, so anything that
// actually decodes video has to ask for them explicitly.
val hostPlatform = rootProject.extra["hostPlatform"] as String

dependencies {
	api(project(":main:boofcv-core"))
	api(project(":integration:boofcv-swing"))

	implementation(project(":integration:boofcv-ffmpeg"))
	implementation(project(":integration:boofcv-jcodec"))
	implementation(project(":integration:boofcv-WebcamCapture"))
	implementation(project(":integration:boofcv-deepboof"))

	implementation(Libs.COMMONS_IO)

	api("org.reflections:reflections:0.10.2") {
		exclude(group = "org.slf4j")
		exclude(group = "com.google.guava")
	}
	runtimeOnly("${Libs.FFMPEG}:$hostPlatform")
}

// Disable ErrorProne since this code needs to be very concise
tasks.withType<JavaCompile>().configureEach {
	options.errorprone.isEnabled.set(false)
}
