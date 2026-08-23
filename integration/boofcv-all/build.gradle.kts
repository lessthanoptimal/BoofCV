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
	api(project(":main:boofcv-core"))

	api(project(":integration:boofcv-swing"))
	api(project(":integration:boofcv-jcodec"))
	api(project(":integration:boofcv-WebcamCapture"))
	api(project(":integration:boofcv-javacv"))
	api(project(":integration:boofcv-ffmpeg"))
}
