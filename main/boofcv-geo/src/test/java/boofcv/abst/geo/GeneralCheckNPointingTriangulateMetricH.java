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

package boofcv.abst.geo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.triangulate.CommonTriangulationChecks;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
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
public abstract class GeneralCheckNPointingTriangulateMetricH extends CommonTriangulationChecks {

	public abstract boolean triangulate( List<Point3D_F64> obsPts, List<Se3_F64> motion,
										 List<DMatrixRMaj> essential,
										 Point4D_F64 found );

	@Test void perfectInput() {
		createScene();

		Point4D_F64 found = new Point4D_F64();
		assertTrue(triangulate(obsPointing, motionWorldToCamera, essential, found));

		assertEquals(0.0, worldPoint.distance(convertH(found)), UtilEjml.TEST_F64_SQ);
	}

	@Test void pointAtInfinity() {
		this.createScene(new Point4D_F64(0.1, -0.2, 4.0, 0.0));

		Point4D_F64 found = new Point4D_F64();
		assertTrue(triangulate(obsPointing, motionWorldToCamera, essential, found));
		assertEquals(0.0, PerspectiveOps.distance(worldPointH, found), UtilEjml.TEST_F64);
	}
}
