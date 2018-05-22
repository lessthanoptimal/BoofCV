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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
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

		NormalizationPoint2D N = new NormalizationPoint2D();
		LowLevelMultiViewOps.computeNormalization(list, N);

		List<Point2D_F64> transformed = new ArrayList<>();
		for( Point2D_F64 p : list ) {
			Point2D_F64 t = new Point2D_F64();
			N.apply(p, t);
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

		NormalizationPoint2D expected1 = new NormalizationPoint2D();
		NormalizationPoint2D expected2 = new NormalizationPoint2D();

		LowLevelMultiViewOps.computeNormalization(list1, expected1);
		LowLevelMultiViewOps.computeNormalization(list2, expected2);

		NormalizationPoint2D found1 = new NormalizationPoint2D();
		NormalizationPoint2D found2 = new NormalizationPoint2D();

		LowLevelMultiViewOps.computeNormalization(list, found1, found2);

		assertTrue(expected1.isEquals(found1,1e-8));
		assertTrue(expected2.isEquals(found2,1e-8));
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

		NormalizationPoint2D expected1 = new NormalizationPoint2D();
		NormalizationPoint2D expected2 = new NormalizationPoint2D();
		NormalizationPoint2D expected3 = new NormalizationPoint2D();

		LowLevelMultiViewOps.computeNormalization(list1, expected1);
		LowLevelMultiViewOps.computeNormalization(list2, expected2);
		LowLevelMultiViewOps.computeNormalization(list3, expected3);

		NormalizationPoint2D found1 = new NormalizationPoint2D();
		NormalizationPoint2D found2 = new NormalizationPoint2D();
		NormalizationPoint2D found3 = new NormalizationPoint2D();

		LowLevelMultiViewOps.computeNormalization(list, found1, found2, found3);

		assertTrue(expected1.isEquals(found1,1e-8));
		assertTrue(expected2.isEquals(found2,1e-8));
		assertTrue(expected3.isEquals(found3,1e-8));
	}
}
