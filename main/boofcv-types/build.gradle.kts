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
	"generateImplementation"(project(":main:autocode"))

	testImplementation(project(":main:boofcv-test"))
}

// Sanity check to make sure autogenerate has been run. This is the source of a lot of
// confusion from users, so make the error as obvious as possible.
tasks.named<JavaCompile>("compileJava") {
	doFirst {
		if (!file("src/main/java/boofcv/struct/border/ImageBorder_F32.java").exists()) {
			throw InvalidUserDataException(
				"*****\n  You must run './gradlew autogenerate' before you can build!\n*****"
			)
		}
	}
}
