/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.pose;

import boofcv.alg.geo.pose.P3PFinsterwalder;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import org.ddogleg.solver.PolynomialOps;
import org.ddogleg.solver.RootFinderType;

/**
 * @author Peter Abeles
 */
public class TestWrapP3PLineDistance extends CheckEstimateNofPnP {
	public TestWrapP3PLineDistance() {
		super(true);

		// arbitrarily select one of the real algorithms
		P3PFinsterwalder finster = new P3PFinsterwalder(PolynomialOps.createRootFinder(4, RootFinderType.STURM));

		WrapP3PLineDistance alg = new WrapP3PLineDistance(finster, FitSpecialEuclideanOps_F64.fitPoints3D());

		setAlgorithm(alg);
	}
}
