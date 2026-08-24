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

// Native platform slices to publish, and the platform of this machine. Set with
// -Pnative_arch=<arch>[,<arch>...]; see the root build.
@Suppress("UNCHECKED_CAST")
val nativePlatforms = rootProject.extra["nativePlatforms"] as List<String>
val hostPlatform = rootProject.extra["hostPlatform"] as String

// Native binaries are published as optional dependencies so downstream users are not forced
// to download every architecture. See the readme in org/bytedeco/copiedstuff.
java {
	registerFeature("natives") { usingSourceSet(sourceSets["main"]) }
}

dependencies {
	api(project(":main:boofcv-ip"))
	api(project(":main:boofcv-io"))

	// Only the FFmpeg presets are needed. The frame grabber below org/bytedeco/copiedstuff is a
	// trimmed copy of JavaCV's, so there is no dependency on org.bytedeco:javacv and the presets
	// it drags in (OpenCV, OpenBLAS, Tesseract, RealSense, Kinect, ...) which this module never
	// uses.
	api(Libs.FFMPEG)

	// Advertised as optional. Which platforms are listed is controlled by -Pnative_arch.
	nativePlatforms.forEach { platform ->
		"nativesRuntimeOnly"("${Libs.FFMPEG}:$platform")
	}

	// BoofCV's own tests decode a real video, so they need real binaries
	testRuntimeOnly("${Libs.FFMPEG}:$hostPlatform")
}
