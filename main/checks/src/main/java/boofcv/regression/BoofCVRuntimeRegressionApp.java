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

package boofcv.regression;

import boofcv.BoofVersion;
import com.peterabeles.ProjectUtils;
import com.peterabeles.regression.RuntimeRegressionMasterApp;

import java.io.File;

public class BoofCVRuntimeRegressionApp {
	public static void main( String[] args ) {
		// Set up the environment
		ProjectUtils.checkRoot = ( f ) ->
				new File(f, "README.md").exists() && new File(f, "settings.gradle").exists();

		ProjectUtils.libraryInfo.version = BoofVersion.VERSION;
		ProjectUtils.libraryInfo.gitDate = BoofVersion.GIT_DATE;
		ProjectUtils.libraryInfo.gitSha = BoofVersion.GIT_SHA;
		ProjectUtils.libraryInfo.projectName = "BoofCV";

		// Specify which packages it should skip over
		String[] excluded = new String[]{"autocode", "checks", "boofcv-types", "boofcv-core"};
		ProjectUtils.skipTest = ( f ) -> {
			for (String name : excluded) {
				if (f.getName().equals(name))
					return true;
			}
			return false;
		};

		RuntimeRegressionMasterApp.main(args);
	}
}
