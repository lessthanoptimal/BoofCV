/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.distort;

import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksPointDeformKeyPoints {

	public abstract PointDeformKeyPoints createAlgorithm();

	/**
	 * Sees if a local copy of inputs points is not made
	 */
	@Test
	public void checkPointsCopied() {
		List<Point2D_F32> src = createTestPoints();
		List<Point2D_F32> dst = createTestPoints();

		PointDeformKeyPoints alg = createAlgorithm();

		alg.setImageShape(80,100);
		alg.setSource(src);
		alg.setSource(dst);

		Point2D_F32 expected = new Point2D_F32();
		alg.compute(12,19.5f, expected);

		for (int i = 0; i < src.size(); i++) {
			src.get(i).x += 2.5;
			dst.get(i).x += 2.5;
		}

		// see if the results change after modifying the input points
		Point2D_F32 found = new Point2D_F32();
		alg.compute(12,19.5f, found);

		assertEquals(expected.x, found.x, GrlConstants.TEST_F32);
		assertEquals(expected.y, found.y, GrlConstants.TEST_F32);
	}

	private List<Point2D_F32> createTestPoints() {
		List<Point2D_F32> src = new ArrayList<>();
		src.add( new Point2D_F32(10,20));
		src.add( new Point2D_F32(15,10));
		src.add( new Point2D_F32(20,30));
		src.add( new Point2D_F32(25,60));
		return src;
	}

	/**
	 * Makes sure modifying a single points is the same as modifying all the points at once
	 */
	@Test
	public void individualSrcSameAsAll() {
		List<Point2D_F32> src = createTestPoints();
		List<Point2D_F32> dst = createTestPoints();

		PointDeformKeyPoints alg = createAlgorithm();
		alg.setImageShape(80,100);
		alg.setSource(src);
		alg.setSource(dst);

		alg.setSource(1,20,25);
		Point2D_F32 expected = new Point2D_F32();
		alg.compute(12,19.5f, expected);

		src.get(1).set(20,25);
		Point2D_F32 found = new Point2D_F32();
		alg.compute(12,19.5f, found);

		assertEquals(expected.x, found.x, GrlConstants.TEST_F32);
		assertEquals(expected.y, found.y, GrlConstants.TEST_F32);
	}

	/**
	 * Makes sure modifying a single points is the same as modifying all the points at once
	 */
	@Test
	public void individualDstSameAsAll() {
		List<Point2D_F32> src = createTestPoints();
		List<Point2D_F32> dst = createTestPoints();

		PointDeformKeyPoints alg = createAlgorithm();
		alg.setImageShape(80,100);
		alg.setSource(src);
		alg.setSource(dst);

		alg.setDestination(1,20,25);
		Point2D_F32 expected = new Point2D_F32();
		alg.compute(12,19.5f, expected);

		dst.get(1).set(20,25);
		Point2D_F32 found = new Point2D_F32();
		alg.compute(12,19.5f, found);

		assertEquals(expected.x, found.x, GrlConstants.TEST_F32);
		assertEquals(expected.y, found.y, GrlConstants.TEST_F32);
	}

}
