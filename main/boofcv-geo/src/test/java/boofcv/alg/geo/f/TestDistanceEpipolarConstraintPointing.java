/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.AssociatedPair3D;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceEpipolarConstraintPointing extends BoofStandardJUnit {

	DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 100, 0.01, 200, 0, 150, 200, 0, 0, 1);

	Se3_F64 worldToCamera = new Se3_F64();

	Point3D_F64 X = new Point3D_F64(0.1, -0.04, 2.3);

	Point3D_F64 p1, p2;
	DMatrixRMaj E, F;

	public TestDistanceEpipolarConstraintPointing() {
		worldToCamera.getT().setTo(0.1, -0.1, 0.2);

		p1 = renderPointing(new Se3_F64());
		p2 = renderPointing(worldToCamera);

		E = MultiViewOps.createEssential(worldToCamera.getR(), worldToCamera.getT(), null);
		F = MultiViewOps.createFundamental(E, K);
	}

	/**
	 * Give it a perfect observation and a noisy one. Perfect should have a smaller distance
	 */
	@Test void basicCheck() {
		var alg = new DistanceEpipolarConstraintPointing();
		alg.setModel(F);

		double perfect = alg.distance(new AssociatedPair3D(p1, p2));

		p1.x += 0.2;
		p1.y += 0.2;

		double noisy = alg.distance(new AssociatedPair3D(p1, p2));

		assertTrue(perfect < noisy*0.1);
	}

	/**
	 * Scale the input and see if that changes the error
	 */
	@Test void checkScaleInvariance() {
		var alg = new DistanceEpipolarConstraintPointing();
		alg.setModel(F);

		p1.x += 0.2;
		p1.y += 0.2;

		double orig = alg.distance(new AssociatedPair3D(p1, p2));

		// rescale the matrix and see if that changes the results
		CommonOps_DDRM.scale(5, F);
		alg.setModel(F);

		double after = alg.distance(new AssociatedPair3D(p1, p2));

		assertEquals(orig, after, 1e-8);
	}

	private Point3D_F64 renderPointing( Se3_F64 worldToCamera ) {
		Point2D_F64 p = PerspectiveOps.renderPixel(worldToCamera, K, X, null);
		double s = 0.4;
		return new Point3D_F64(p.x*s, p.y*s, s);
	}
}
