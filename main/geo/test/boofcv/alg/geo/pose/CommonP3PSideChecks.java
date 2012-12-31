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

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class CommonP3PSideChecks {

	public abstract List<PointDistance3> computeSolutions( Point2D_F64 obs1 , Point2D_F64 obs2, Point2D_F64 obs3,
														   double length23 , double length13 , double length12,
														   boolean shouldSucceed );


	@Test
	public void basicTest() {

		Point3D_F64 P1 = new Point3D_F64(-0.2,0.4,2);
		Point3D_F64 P2 = new Point3D_F64(0.5,0,2.6);
		Point3D_F64 P3 = new Point3D_F64(-0.4,-0.3,3);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), null, P1);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(new Se3_F64(),null,P2);
		Point2D_F64 p3 = PerspectiveOps.renderPixel(new Se3_F64(),null,P3);

		double length12 = P1.distance(P2);
		double length23 = P2.distance(P3);
		double length13 = P1.distance(P3);

		List<PointDistance3> solutions = computeSolutions(p1,p2,p3,length23,length13,length12,true);

		int numCorrect = 0;
		double tol = 1e-8;

		double expected1 = P1.norm();
		double expected2 = P2.norm();
		double expected3 = P3.norm();

//		System.out.println(expected1+" "+expected2+" "+expected3);
//		System.out.println("--------------");

		for( PointDistance3 s : solutions ) {
//			System.out.println(s.dist1+" "+s.dist2+" "+s.dist3);
			if( Math.abs(s.dist1-expected1) < tol &&
					Math.abs(s.dist2-expected2) < tol &&
					Math.abs(s.dist3-expected3) < tol ) {
				numCorrect++;
			}
		}

		assertTrue(numCorrect >= 1);
	}

	/**
	 * Check a pathological case where everything is zero
	 */
	@Test
	public void pathological1() {
		Point3D_F64 P1 = new Point3D_F64();
		Point3D_F64 P2 = new Point3D_F64();
		Point3D_F64 P3 = new Point3D_F64();

		Point2D_F64 p1 = new Point2D_F64();
		Point2D_F64 p2 = new Point2D_F64();
		Point2D_F64 p3 = new Point2D_F64();

		double length12 = P1.distance(P2);
		double length23 = P2.distance(P3);
		double length13 = P1.distance(P3);

		List<PointDistance3> solutions = computeSolutions(p1,p2,p3,length23,length13,length12,false);

		assertEquals(0,solutions.size());
	}
}
