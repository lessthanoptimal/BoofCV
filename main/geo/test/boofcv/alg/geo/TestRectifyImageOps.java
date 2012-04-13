/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRectifyImageOps {

	Point2D_F32 p = new Point2D_F32();
	int width = 300;
	int height = 350;

	@Test
	public void fullViewLeft_calibrated() {

		IntrinsicParameters param = new IntrinsicParameters(300,300,0,150,150,width,height,new double[]{0.1,1e-4});

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = UtilEpipolar.calibrationMatrix(param);

		RectifyImageOps.fullViewLeft(param,false,rect1,rect2,rectK);

		// check left image
		PointTransform_F32 tran = RectifyImageOps.rectifyTransform(param, false, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}

	private void checkInside(PointTransform_F32 tran) {
		for( int y = 0; y < height; y++ ) {
			checkInside(0,y,tran);
			checkInside(width-1,y,tran);
		}

		for( int x = 0; x < width; x++ ) {
			checkInside(x,0,tran);
			checkInside(x,height-1,tran);
		}
	}

	private void checkInside( int x , int y , PointTransform_F32 tran ) {
		tran.compute(x, y, p);

		float tol = 0.1f;

		String s = x+" "+y+" -> "+p.x+" "+p.y;
		assertTrue(s,p.x >= -tol && p.x < width+tol );
		assertTrue(s,p.y >= -tol && p.y < height+tol );
	}

	@Test
	public void allInsideLeft_calibrated() {
		IntrinsicParameters param = new IntrinsicParameters(300,300,0,150,150,width,height,new double[]{0.1,1e-4});

		// do nothing rectification
		DenseMatrix64F rect1 = CommonOps.identity(3);
		DenseMatrix64F rect2 = CommonOps.identity(3);
		DenseMatrix64F rectK = UtilEpipolar.calibrationMatrix(param);

		RectifyImageOps.allInsideLeft(param, false, rect1, rect2, rectK);

		// check left image
		PointTransform_F32 tran = RectifyImageOps.rectifyTransformInv(param, false, rect1);
		checkInside(tran);
		// the right view is not checked since it is not part of the contract
	}
}
