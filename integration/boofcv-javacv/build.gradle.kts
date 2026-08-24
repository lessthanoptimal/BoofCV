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

// Native platform slices to publish. Set with -Pnative_arch=<arch>[,<arch>...].
@Suppress("UNCHECKED_CAST")
val nativePlatforms = rootProject.extra["nativePlatforms"] as List<String>
val hostPlatform = rootProject.extra["hostPlatform"] as String

// Native binaries are published as optional dependencies so downstream users are not forced
// to download every architecture.
java {
	registerFeature("natives") { usingSourceSet(sourceSets["main"]) }
}

dependencies {
	api(project(":main:boofcv-ip"))
	api(project(":main:boofcv-io"))
	api(project(":main:boofcv-geo"))

	api(project(":integration:boofcv-swing"))

	// Unlike boofcv-ffmpeg, this module genuinely needs JavaCV itself: WebcamOpenCV drives
	// OpenCVFrameGrabber for live capture. Only pure Java jars arrive this way; the native
	// binaries are declared below.
	//
	// The exclusions drop the presets JavaCV depends on but this module never touches.
	// OpenCVFrameGrabber references org.bytedeco.opencv and nothing else. The capture device
	// ones are also still pinned to javacpp 1.5.9, which collides with 1.5.14 under
	// failOnVersionConflict(), so excluding them fixes that at the source rather than forcing a
	// version, and keeps the published POM from advertising Kinect, RealSense and an OCR engine.
	api(Libs.JAVACV) {
		listOf("flycapture", "libdc1394", "libfreenect", "libfreenect2", "librealsense",
			"librealsense2", "videoinput", "artoolkitplus", "tesseract", "leptonica").forEach {
			exclude(group = "org.bytedeco", module = it)
		}
	}

	// Advertised as optional. Which platforms are listed is controlled by -Pnative_arch.
	// OpenCV 4.x links against OpenBLAS, so both are needed together.
	nativePlatforms.forEach { platform ->
		"nativesRuntimeOnly"("${Libs.OPENCV}:$platform")
		"nativesRuntimeOnly"("${Libs.OPENBLAS}:$platform")
	}

	// BoofCV's own tests exercise the native library, so they need real binaries
	testRuntimeOnly("${Libs.OPENCV}:$hostPlatform")
	testRuntimeOnly("${Libs.OPENBLAS}:$hostPlatform")
}

sourceSets { test { java.srcDir("src/main/examples") } }
