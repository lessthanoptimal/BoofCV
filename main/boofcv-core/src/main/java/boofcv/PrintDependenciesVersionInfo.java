/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import georegression.GeoRegressionVersion;
import org.ddogleg.DDoglegVersion;
import org.ejml.EjmlVersion;

/**
 * Prints version information on included dependencies. Useful for debugging, especially when dealing with SNAPSHOTs
 *
 * @author Peter Abeles
 */
public class PrintDependenciesVersionInfo {
	public static void main( String[] args ) {
		System.out.println("------------------ Dependency Version Info --------------------------");
		System.out.printf("%20s version=%15s Dirty=%1d Build_Date=%10s SHA=%s\n","EJML",
				EjmlVersion.VERSION, EjmlVersion.DIRTY, EjmlVersion.BUILD_DATE, EjmlVersion.GIT_SHA);
		System.out.printf("%20s version=%15s Dirty=%1d Build_Date=%10s SHA=%s\n","DDogleg",
				DDoglegVersion.VERSION, DDoglegVersion.DIRTY, DDoglegVersion.BUILD_DATE, DDoglegVersion.GIT_SHA);
		System.out.printf("%20s version=%15s Dirty=%1d Build_Date=%10s SHA=%s\n","GeoRegression",
				GeoRegressionVersion.VERSION, GeoRegressionVersion.DIRTY, GeoRegressionVersion.BUILD_DATE, GeoRegressionVersion.GIT_SHA);
//		System.out.printf("%20s version=%20s Dirty=%1d Build_Date=%10s SHA=%s\n","DeepBoof",
//				GeoRegressionVersion.VERSION, GeoRegressionVersion.DIRTY, GeoRegressionVersion.BUILD_DATE, GeoRegressionVersion.GIT_SHA);
	}
}
