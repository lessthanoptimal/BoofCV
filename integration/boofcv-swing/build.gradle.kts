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

dependencies {
	api(project(":main:boofcv-types"))
	api(project(":main:boofcv-io"))
	api(project(":main:boofcv-feature"))
	api(project(":main:boofcv-geo"))
	api(project(":main:boofcv-sfm"))
	api(project(":main:boofcv-recognition"))
	api("com.fifesoft:rsyntaxtextarea:2.6.1")
	api("io.github.vincenzopalazzo:material-ui-swing:1.1.1_pre-release_6.1")
	api("com.github.weisj:darklaf-core:1.4.3.1")

	implementation(Libs.SNAKEYAML)
	implementation(Libs.COMMONS_IO)
}
