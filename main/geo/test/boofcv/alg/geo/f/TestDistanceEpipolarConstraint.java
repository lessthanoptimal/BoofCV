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

package boofcv.alg.geo.f;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceEpipolarConstraint {

	DenseMatrix64F K = new DenseMatrix64F(3,3,true,100,0.01,200,0,150,200,0,0,1);

	Se3_F64 worldToCamera = new Se3_F64();

	Point3D_F64 X = new Point3D_F64(0.1,-0.04,2.3);

	Point2D_F64 p1,p2;
	DenseMatrix64F E,F;

	public TestDistanceEpipolarConstraint() {
		worldToCamera.getT().set(0.1,-0.1,0.2);

		p1 = PerspectiveOps.renderPixel(new Se3_F64(),K,X);
		p2 = PerspectiveOps.renderPixel(worldToCamera,K,X);

		E = MultiViewOps.createEssential(worldToCamera.getR(),worldToCamera.getT());
		F = MultiViewOps.createFundamental(E,K);
	}

	/**
	 * Give it a perfect observation and a noisy one.  Perfect should have a smaller distance
	 */
	@Test
	public void basicCheck() {


		DistanceEpipolarConstraint alg = new DistanceEpipolarConstraint();
		alg.setModel(F);

		double perfect = alg.computeDistance(new AssociatedPair(p1,p2));

		p1.x += 0.2;
		p1.y += 0.2;

		double noisy = alg.computeDistance(new AssociatedPair(p1,p2));

		assertTrue( perfect < noisy*0.1 );
	}

	/**
	 * Scale the input and see if that changes the error
	 */
	@Test
	public void checkScaleInvariance() {
		DistanceEpipolarConstraint alg = new DistanceEpipolarConstraint();
		alg.setModel(F);

		p1.x += 0.2;
		p1.y += 0.2;

		double orig = alg.computeDistance(new AssociatedPair(p1,p2));

		// rescale the matrix and see if that changes the results
		CommonOps.scale(5,F);
		alg.setModel(F);

		double after = alg.computeDistance(new AssociatedPair(p1,p2));

		assertEquals(orig,after,1e-8);
	}
}
