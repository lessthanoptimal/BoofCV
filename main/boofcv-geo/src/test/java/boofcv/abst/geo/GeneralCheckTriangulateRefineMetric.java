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

package boofcv.abst.geo;

import boofcv.alg.geo.triangulate.CommonTriangulationChecks;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralCheckTriangulateRefineMetric extends CommonTriangulationChecks {

	public abstract void triangulate( List<Point2D_F64> obsPts, List<Se3_F64> motion,
									  List<DMatrixRMaj> essential,
									  Point3D_F64 initial, Point3D_F64 found );

	@Test void perfectInput() {
		createScene();

		Point3D_F64 initial = worldPoint.copy();
		Point3D_F64 found = new Point3D_F64();
		triangulate(obsNorm, motionWorldToCamera, essential, initial, found);

		assertEquals(0.0, worldPoint.distance(found), UtilEjml.TEST_F64_SQ);
	}

	@Test void incorrectInput() {
		createScene();

		Point3D_F64 initial = worldPoint.copy();
		initial.x += 0.01;
		initial.y += 0.1;
		initial.z += -0.05;

		Point3D_F64 found = new Point3D_F64();
		triangulate(obsNorm, motionWorldToCamera, essential, initial, found);

		double error2 = worldPoint.distance(initial);
		double error = worldPoint.distance(found);

		assertTrue(error < error2*0.5);
	}
}
