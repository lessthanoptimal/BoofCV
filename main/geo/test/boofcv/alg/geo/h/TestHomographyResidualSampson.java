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

package boofcv.alg.geo.h;

import boofcv.alg.geo.ModelObservationResidualN;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHomographyResidualSampson extends HomographyResidualTests {

	@Override
	public ModelObservationResidualN createAlg() {
		return new HomographyResidualSampson();
	}

	
	@Test
	public void checkJacobian() {
		HomographyResidualSampson alg = new HomographyResidualSampson();

		DenseMatrix64F H = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);
		Point2D_F64 x1 = new Point2D_F64(10,20);
		Point2D_F64 x2 = new Point2D_F64(30,40);

		DenseMatrix64F J = new DenseMatrix64F(2,4);
		alg.H = H;

		// see page 130,  the cost function is multilinear and the jacobian can be computed this way
		double top1 = alg.error1(x1.x, x1.y, x2.x, x2.y);
		double top2 = alg.error2(x1.x, x1.y, x2.x, x2.y);

		J.data[0] = alg.error1(x1.x + 1, x1.y, x2.x, x2.y) - top1;
		J.data[1] = alg.error1(x1.x,x1.y+1,x2.x,x2.y) - top1;
		J.data[2] = alg.error1(x1.x,x1.y,x2.x+1,x2.y) - top1;
		J.data[3] = alg.error1(x1.x,x1.y,x2.x,x2.y+1) - top1;
		J.data[4] = alg.error2(x1.x + 1, x1.y, x2.x, x2.y) - top2;
		J.data[5] = alg.error2(x1.x, x1.y + 1, x2.x, x2.y) - top2;
		J.data[6] = alg.error2(x1.x, x1.y, x2.x + 1, x2.y) - top2;
		J.data[7] = alg.error2(x1.x, x1.y, x2.x, x2.y + 1) - top2;

		alg.computeJacobian(x1,x2);
		
		assertTrue(MatrixFeatures.isEquals(J, alg.J, 1e-8));

	}
}
