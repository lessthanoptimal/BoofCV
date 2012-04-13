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
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestUtilIntrinsic {

	@Test
	public void adjustDistortion_F32() {
		fail("implement");
	}

	@Test
	public void calibrationMatrix() {
		DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(1, 2, 3, 4, 5);

		assertEquals(1,K.get(0,0),1e-3);
		assertEquals(2,K.get(1,1),1e-3);
		assertEquals(3,K.get(0,1),1e-3);
		assertEquals(4,K.get(0,2),1e-3);
		assertEquals(5,K.get(1,2),1e-3);
		assertEquals(1,K.get(2,2),1e-3);
	}

	@Test
	public void matrixToParam() {
		double fx = 1;
		double fy = 2;
		double skew = 3;
		double cx = 4;
		double cy = 5;

		DenseMatrix64F K = new DenseMatrix64F(3,3,true,fx,skew,cx,0,fy,cy,0,0,1);
		IntrinsicParameters ret = UtilIntrinsic.matrixToParam(K, 100, 200, null);

		assertTrue(ret.fx == fx);
		assertTrue(ret.fy == fy);
		assertTrue(ret.skew == skew);
		assertTrue(ret.cx == cx);
		assertTrue(ret.cy == cy);
		assertTrue(ret.width == 100);
		assertTrue(ret.height == 200);
	}
}
