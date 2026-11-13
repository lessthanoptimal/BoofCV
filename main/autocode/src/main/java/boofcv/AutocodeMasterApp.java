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

package boofcv;

import java.io.File;
import java.io.IOException;

/// Master application which calls all auto code generating classes.
public class AutocodeMasterApp {
	/// Finds the project root by searching the working directory and its parents
	public static File findPathToProjectRoot() {
		return findPathToProjectRoot(new File(""));
	}

	/// Finds the project root by searching start and its parents. The root is the first directory with "main"
	/// and "gradle.properties" in it.
	///
	/// @throws RuntimeException If no parent is the project root
	public static File findPathToProjectRoot( File start ) {
		for (File d = start.getAbsoluteFile(); d != null; d = d.getParentFile()) {
			if (new File(d, "main").exists() && new File(d, "gradle.properties").exists())
				return d;
		}
		throw new RuntimeException("Can't find the project root in or above " + start.getAbsolutePath());
	}

	public static void main( String[] args ) throws IOException {
		Autocode64to32App.main(new String[0]);
		AutocodeConcurrentApp.main(new String[0]);
	}
}
