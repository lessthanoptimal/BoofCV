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

package boofcv.alg.geo;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilCurves_F64;
import georegression.misc.GrlConstants;
import georegression.struct.curve.ConicGeneral_F64;
import georegression.struct.curve.ParabolaGeneral_F64;
import georegression.struct.curve.ParabolaParametric_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestNormalizationPoint2D extends BoofStandardJUnit {
	@Test
	void apply_2D() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		Point2D_F64 a = new Point2D_F64(1,2);

		n.apply(a,a);
		n.remove(a,a);

		assertEquals(1,a.x, GrlConstants.TEST_F64);
		assertEquals(2,a.y, GrlConstants.TEST_F64);
	}

	@Test
	void apply_3D() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		Point3D_F64 a = new Point3D_F64(1,2,3.5);

		n.apply(a,a);
		n.remove(a,a);

		assertEquals(1,a.x, GrlConstants.TEST_F64);
		assertEquals(2,a.y, GrlConstants.TEST_F64);
		assertEquals(3.5,a.z, GrlConstants.TEST_F64);
	}

	@Test
	void apply_conic() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		// finding points on an arbitrary conic isn't trivial.
		ParabolaGeneral_F64 parabola = new ParabolaGeneral_F64(1,2,3.5,0.1,0.01);
		ParabolaParametric_F64 parametric = new ParabolaParametric_F64();
		ConicGeneral_F64 conic = new ConicGeneral_F64();
		UtilCurves_F64.convert(parabola,conic);
		UtilCurves_F64.convert(parabola,parametric);

		List<Point2D_F64> points = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			points.add(parametric.evaluate(i*0.1));
		}
		ConicGeneral_F64 conicN = new ConicGeneral_F64();
		n.apply(conic,conicN);

		// verify that the formula doesn't get messed up in the transform
		points.forEach(t->{
			// sanity check
			assertEquals(0,conic.evaluate(t.x,t.y), UtilEjml.TEST_F64);
			// transform the point
			t = t.copy();
			n.apply(t,t);
			// should be zero in the normalized conic
			assertEquals(0,conicN.evaluate(t.x,t.y), UtilEjml.TEST_F64);
		});

		// reverse and see if all is good
		n.remove(conicN,conic);
		points.forEach(t->assertEquals(0,conic.evaluate(t.x,t.y), UtilEjml.TEST_F64));
	}

	@Test
	void apply_matrix() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		DMatrixRMaj H = new DMatrixRMaj(new double[][]{{1,2,3,9},{3,4,5,8},{5,6,7,7}});
		DMatrixRMaj H_orig = H.copy();
		DMatrixRMaj N = n.matrix(null);

		DMatrixRMaj expected = new DMatrixRMaj(3,4);
		CommonOps_DDRM.mult(N,H,expected);

		n.apply(H,H);

		assertTrue(MatrixFeatures_DDRM.isIdentical(expected,H, UtilEjml.TEST_F64));

		// see if remove works
		n.remove(H,H);
		assertTrue(MatrixFeatures_DDRM.isIdentical(H_orig,H, UtilEjml.TEST_F64));
	}

	@Test
	void matrixInv() {
		NormalizationPoint2D n = new NormalizationPoint2D(1,2,3,4);

		DMatrixRMaj A = n.matrix(null);
		DMatrixRMaj B = n.matrixInv(null);

		DMatrixRMaj C = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(A,B,C);

		assertTrue(MatrixFeatures_DDRM.isIdentity(C,GrlConstants.TEST_F64));
	}

	@Test
	void isEquals() {
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
