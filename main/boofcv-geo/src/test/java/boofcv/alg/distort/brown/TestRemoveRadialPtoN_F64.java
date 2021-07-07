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

package boofcv.alg.distort.brown;

import boofcv.alg.distort.Transform2ThenPixel_F64;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRemoveRadialPtoN_F64 extends BoofStandardJUnit {

	@Test void checkAgainstAdd() {
		checkAgainstAdd(0,0);
		checkAgainstAdd(0.1,-0.05);
	}

	public void checkAgainstAdd( double t1 , double t2 ) {
		double fx = 600;
		double fy = 500;
		double skew = 2;
		double xc = 300;
		double yc = 350;

		/**/double[] radial= new /**/double[]{0.12,-0.13};

		Point2D_F64 point = new Point2D_F64();

		double undistX = 19.5;
		double undistY = 200.1;

		AddBrownPtoN_F64 p_to_n = new AddBrownPtoN_F64().setK(fx, fy, skew, xc, yc).setDistortion(radial,t1,t2);
		new Transform2ThenPixel_F64(p_to_n).set(fx, fy, skew, xc, yc).compute(undistX, undistY, point);

		double distX = point.x;
		double distY = point.y;

		RemoveBrownPtoN_F64 alg = new RemoveBrownPtoN_F64().setK(fx,fy,skew,xc,yc).setDistortion(radial,t1,t2);

		alg.compute(distX, distY, point);

		/// go from calibrated coordinates to pixel
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(fx, fy, skew, xc, yc);

		GeometryMath_F64.mult(K,point,point);

		assertEquals(undistX,point.x, GrlConstants.TEST_SQ_F64);
		assertEquals(undistY,point.y, GrlConstants.TEST_SQ_F64);
	}
}
