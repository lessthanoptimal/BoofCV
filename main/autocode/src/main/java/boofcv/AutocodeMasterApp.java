/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import java.nio.file.Paths;

/**
 * Master application which calls all auto code generating classes.
 *
 * @author Peter Abeles
 */
public class AutocodeMasterApp {
	public static File findPathToProjectRoot() {
		String path = "./";
		while (true) {
			File d = new File(path);
			if (new File(d, "main").exists() && new File(d, "gradle.properties").exists())
				break;
			path = "../" + path;
		}

		// normalize().toFile() messes up in this situation and makes it relative to "/"
		if (path.equals("./"))
			return new File(path);

		return Paths.get(path).normalize().toFile();
	}

	public static void main( String[] args ) throws IOException {
		Autocode64to32App.main(new String[0]);
		AutocodeConcurrentApp.main(new String[0]);
	}
}
