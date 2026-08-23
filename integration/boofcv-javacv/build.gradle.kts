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

// Native platform slices to publish. Set with -Pnative_arch=<arch>; see the root build.
@Suppress("UNCHECKED_CAST")
val nativeArch = rootProject.extra["nativeArch"] as List<String>

dependencies {
	api(project(":main:boofcv-ip"))
	api(project(":main:boofcv-io"))
	api(project(":main:boofcv-geo"))

	api(project(":integration:boofcv-swing"))

	api("org.bytedeco:javacv:1.4.4")

	nativeArch.forEach { arch ->
		implementation("org.bytedeco.javacpp-presets:opencv:4.0.1-1.4.4:$arch")
	}
}

sourceSets { test { java.srcDir("src/main/examples") } }
