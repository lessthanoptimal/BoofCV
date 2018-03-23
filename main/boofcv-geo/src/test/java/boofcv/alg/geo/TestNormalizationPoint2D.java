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

package boofcv.alg.geo;

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestNormalizationPoint2D {
	@Test
	public void apply_one() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		Point2D_F64 a = new Point2D_F64(1,2);

		n.apply(a);
		n.remove(a);

		assertEquals(1,a.x, GrlConstants.TEST_F64);
		assertEquals(2,a.y, GrlConstants.TEST_F64);
	}

	@Test
	public void apply_two() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		Point2D_F64 a = new Point2D_F64(1,2);
		Point2D_F64 b = new Point2D_F64();
		Point2D_F64 c = new Point2D_F64();

		n.apply(a,b);
		// make sure no change in a
		assertEquals(1,a.x, GrlConstants.TEST_F64);
		assertEquals(2,a.y, GrlConstants.TEST_F64);
		n.remove(b,c);

		assertEquals(1,c.x, GrlConstants.TEST_F64);
		assertEquals(2,c.y, GrlConstants.TEST_F64);
	}

	@Test
	public void matrixInv() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		DMatrixRMaj A = n.matrix();
		DMatrixRMaj B = n.matrixInv();

		DMatrixRMaj C = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(A,B,C);

		assertTrue(MatrixFeatures_DDRM.isIdentity(C,GrlConstants.TEST_F64));
	}

	@Test
	public void isEquals() {
		NormalizationPoint2D a = new NormalizationPoint2D(1,2,3,4);
		NormalizationPoint2D b = new NormalizationPoint2D(1,2,3,4);

		assertTrue(a.isEquals(b,0));

		a.meanX += 1e-5;
		assertFalse(a.isEquals(b,1e-8));
		a.meanX = 1;
		a.meanY += 1e-5;
		assertFalse(a.isEquals(b,1e-8));
		a.meanY = 3;
		a.stdX += 1e-5;
		assertFalse(a.isEquals(b,1e-8));
		a.stdX = 2;
		a.stdY += 1e-5;
		assertFalse(a.isEquals(b,1e-8));
	}


}