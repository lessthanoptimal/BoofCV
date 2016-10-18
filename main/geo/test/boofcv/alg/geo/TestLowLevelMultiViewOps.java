/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLowLevelMultiViewOps {

	Random rand = new Random(345);

	@Test
	public void computeNormalization() {
		List<Point2D_F64> list = new ArrayList<>();
		for( int i = 0; i < 12; i++ ) {
			Point2D_F64 p = new Point2D_F64();

			p.set(rand.nextDouble()*5,rand.nextDouble()*5);

			list.add(p);
		}

		DenseMatrix64F N = new DenseMatrix64F(3,3);
		LowLevelMultiViewOps.computeNormalization(list, N);

		List<Point2D_F64> transformed = new ArrayList<>();
		for( Point2D_F64 p : list ) {
			Point2D_F64 t = new Point2D_F64();
			GeometryMath_F64.mult(N, p, t);
			transformed.add(t);
		}

		// see if the transformed points have the expected statistical properties
		double meanX0 = 0;
		double meanY0 = 0;

		for( Point2D_F64 p : transformed ) {
			meanX0 += p.x;
			meanY0 += p.y;
		}

		meanX0 /= list.size();
		meanY0 /= list.size();

		assertEquals(0,meanX0,1e-8);
		assertEquals(0,meanY0,1e-8);

		double sigmaX0 = 0;
		double sigmaY0 = 0;

		for( Point2D_F64 p : transformed ) {
			sigmaX0 += Math.pow(p.x-meanX0,2);
			sigmaY0 += Math.pow(p.y-meanY0,2);
		}

		sigmaX0 = Math.sqrt(sigmaX0/list.size());
		sigmaY0 = Math.sqrt(sigmaY0/list.size());

		assertEquals(1,sigmaX0,1e-8);
		assertEquals(1,sigmaY0,1e-8);

	}

	/**
	 * Compare to single list function
	 */
	@Test
	public void computeNormalization_two() {

		List<AssociatedPair> list = new ArrayList<>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedPair p = new AssociatedPair();

			p.p2.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.p1.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list,list1,list2);

		DenseMatrix64F expected1 = new DenseMatrix64F(3,3);
		DenseMatrix64F expected2 = new DenseMatrix64F(3,3);

		LowLevelMultiViewOps.computeNormalization(list1, expected1);
		LowLevelMultiViewOps.computeNormalization(list2, expected2);

		DenseMatrix64F found1 = new DenseMatrix64F(3,3);
		DenseMatrix64F found2 = new DenseMatrix64F(3,3);

		LowLevelMultiViewOps.computeNormalization(list, found1, found2);

		assertTrue(MatrixFeatures.isIdentical(expected1, found1, 1e-8));
		assertTrue(MatrixFeatures.isIdentical(expected2,found2,1e-8));
	}

	/**
	 * Compare to single list function
	 */
	@Test
	public void computeNormalization_three() {

		List<AssociatedTriple> list = new ArrayList<>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedTriple p = new AssociatedTriple();

			p.p1.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.p2.set(rand.nextDouble() * 5, rand.nextDouble() * 5);
			p.p3.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();
		List<Point2D_F64> list3 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list,list1,list2,list3);

		DenseMatrix64F expected1 = new DenseMatrix64F(3,3);
		DenseMatrix64F expected2 = new DenseMatrix64F(3,3);
		DenseMatrix64F expected3 = new DenseMatrix64F(3,3);

		LowLevelMultiViewOps.computeNormalization(list1, expected1);
		LowLevelMultiViewOps.computeNormalization(list2, expected2);
		LowLevelMultiViewOps.computeNormalization(list3, expected3);

		DenseMatrix64F found1 = new DenseMatrix64F(3,3);
		DenseMatrix64F found2 = new DenseMatrix64F(3,3);
		DenseMatrix64F found3 = new DenseMatrix64F(3,3);

		LowLevelMultiViewOps.computeNormalization(list, found1, found2, found3);

		assertTrue(MatrixFeatures.isIdentical(expected1,found1,1e-8));
		assertTrue(MatrixFeatures.isIdentical(expected2,found2,1e-8));
		assertTrue(MatrixFeatures.isIdentical(expected3,found3,1e-8));
	}

	/**
	 * Test it against a simple test case
	 */
	@Test
	public void applyPixelNormalization() {
		DenseMatrix64F N = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);

		Point2D_F64 a = new Point2D_F64(3,4);
		Point2D_F64 found = new Point2D_F64(3,4);
		Point2D_F64 expected = new Point2D_F64(3,4);

		expected.x = a.x * N.get(0,0) + N.get(0,2);
		expected.y = a.y * N.get(1,1) + N.get(1,2);


		LowLevelMultiViewOps.applyPixelNormalization(N, a, found);

		assertEquals(found.x,expected.x,1e-8);
		assertEquals(found.y,expected.y,1e-8);
	}
}
