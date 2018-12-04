/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralTestRefineTriangulateProjective extends CommonTriangulationChecks {

	public abstract void triangulate(List<Point2D_F64> obsPts,
									 List<DMatrixRMaj> cameraMatrices ,
									 Point4D_F64 initial , Point4D_F64 found );

	@Test
	public void perfectInput() {
		createScene();

		Point4D_F64 initial = convert(worldPoint);
		Point4D_F64 found = new Point4D_F64();

		triangulate(obsPts, cameraMatrices,initial,found);

		assertEquals(worldPoint.x, convert(found).x, 1e-8);
	}

	@Test
	public void incorrectInput() {
		createScene();

		Point4D_F64 initial = convert(worldPoint);
		initial.x += 0.01;
		initial.y += 0.1;
		initial.z += -0.05;
		initial.w *= 1.1;

		Point4D_F64 found = new Point4D_F64();
		triangulate(obsPts, cameraMatrices,initial,found);

		double error2 = worldPoint.distance(convert(initial));
		double error = worldPoint.distance(convert(found));

		assertTrue(error < error2 * 0.5);
	}

	private Point3D_F64 convert( Point4D_F64 X ) {
		return new Point3D_F64(X.x/X.w,X.y/X.w,X.z/X.w);
	}

	private Point4D_F64 convert( Point3D_F64 X ) {
		double scale = rand.nextGaussian();
		if( Math.abs(scale) < 1e-5 )
			scale = 0.001;
		Point4D_F64 P = new Point4D_F64();
		P.x = X.x * scale;
		P.y = X.y * scale;
		P.z = X.z * scale;
		P.w = scale;
		return P;
	}
}
