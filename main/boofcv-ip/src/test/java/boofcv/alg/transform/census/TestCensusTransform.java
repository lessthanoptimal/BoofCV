/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests sampling functions and which bit each sample sets. The transforms are compared against a brute force
/// implementation in GCensusTransform
class TestCensusTransform extends BoofStandardJUnit {
	@Test
	void createBlockSamples_1() {
		DogArray<Point2D_I32> samples = CensusTransform.createBlockSamples(2);
		assertEquals(5*5-1, samples.size);
		for (int y = -2, i=0; y < 3; y++) {
			for (int x = -2; x < 3; x++) {
				if( x == 0 && y == 0)
					continue;
				assertTrue(samples.get(i++).distance(x,y) <= UtilEjml.EPS);
			}
		}
	}

	@Test
	void createBlockSamples_2() {
		DogArray<Point2D_I32> samples = CensusTransform.createBlockSamples(1,2);
		assertEquals(3*5-1, samples.size);
		for (int y = -2, i=0; y < 3; y++) {
			for (int x = -1; x < 2; x++) {
				if( x == 0 && y == 0)
					continue;
				assertTrue(samples.get(i++).distance(x,y) <= UtilEjml.EPS);
			}
		}
	}

	@Test
	void createCircleSamples() {
		DogArray<Point2D_I32> samples = CensusTransform.createCircleSamples();
		assertEquals(9 * 9 - 4 * 6 - 1, samples.size);

		// make sure there's no zero
		for (Point2D_I32 p : samples.toList()) {
			assertFalse(p.x == 0 && p.y == 0);
		}

		// make sure no point is more than 4 away and the mean is zero
		int meanX = 0;
		int meanY = 0;
		for (Point2D_I32 p : samples.toList()) {
			assertFalse(p.distance(0, 0) > 4.5);
			meanX += p.x;
			meanY += p.y;
		}
		assertEquals(0, meanX);
		assertEquals(0, meanY);
	}

	/// Only one sample is brighter than the center pixel, so only its bit is set. Samples 31 and above need a 64-bit
	/// mask. Checks a pixel in the inner image and one in the border.
	@Test
	void sample_S64_bitOrder() {
		DogArray<Point2D_I32> samples = CensusTransform.createBlockSamples(3);
		assertEquals(48, samples.size);

		for (Class type : new Class[]{GrayU8.class, GrayU16.class, GrayF32.class}) {
			for (int index : new int[]{0, 30, 31, 32, 47}) {
				// inner and border center pixels. Sample 0 is at (-3,-3), the others have positive offsets
				int[][] centers = index == 0 ? new int[][]{{10, 10}, {18, 18}} : new int[][]{{10, 10}, {1, 1}};
				for (int[] c : centers) {
					ImageGray input = GeneralizedImageOps.createSingleBand(type, 20, 20);
					GImageMiscOps.fill(input, 10);
					Point2D_I32 p = samples.get(index);
					GeneralizedImageOps.set(input, c[0] + p.x, c[1] + p.y, 20);

					var output = new GrayS64(1, 1);
					GCensusTransform.sample_S64(input, samples, output,
							FactoryImageBorder.wrap(BorderType.EXTENDED, input), null);
					assertEquals(1L << index, output.get(c[0], c[1]),
							type.getSimpleName() + " index=" + index + " at " + c[0] + "," + c[1]);
				}
			}
		}
	}
}
