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

package boofcv.alg.geo.pose;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.solver.PolynomialOps;
import org.ddogleg.solver.RootFinderType;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestP3PGrunert extends CommonP3PSideChecks {

	P3PGrunert alg = new P3PGrunert(PolynomialOps.createRootFinder(4, RootFinderType.EVD));

	@Override
	public List<PointDistance3> computeSolutions(Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3,
												 double length23, double length13, double length12,
												 boolean shouldSucceed) {
		assertTrue(alg.process(p1,p2,p3,length23,length13,length12)==shouldSucceed);

		return alg.getSolutions().toList();
	}

}
