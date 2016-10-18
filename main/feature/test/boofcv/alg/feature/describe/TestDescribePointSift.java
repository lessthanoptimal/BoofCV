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

package boofcv.alg.feature.describe;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribePointSift {

	Random rand = new Random(234);

	/**
	 * Tests to see if it blows up and not much more.  Random image.  Compute descriptor along border and image
	 * center.
	 */
	@Test
	public void process() {
		GrayF32 derivX = new GrayF32(200,200);
		GrayF32 derivY = new GrayF32(200,200);

		GImageMiscOps.fillUniform(derivX,rand,-100,100);
		GImageMiscOps.fillUniform(derivY,rand,-100,100);

		DescribePointSift<GrayF32> alg =
				new DescribePointSift<>(4,4,8,1.5,0.5,0.2,GrayF32.class);
		alg.setImageGradient(derivX,derivY);

		List<Point2D_I32> testPoints = new ArrayList<>();
		testPoints.add( new Point2D_I32(100,0));
		testPoints.add( new Point2D_I32(100,199));
		testPoints.add( new Point2D_I32(0,100));
		testPoints.add( new Point2D_I32(199,100));
		testPoints.add( new Point2D_I32(100,100));

		TupleDesc_F64 desc = new TupleDesc_F64(alg.getDescriptorLength());
		for( Point2D_I32 where : testPoints ) {
			alg.process(where.x,where.y,2,0.5,desc);
		}
	}

	/**
	 * Only put gradient inside a small area that fills the descriptor.  Then double the scale and see if
	 * only a 1/4 of the original image is inside
	 */
	@Test
	public void computeRawDescriptor_scale() {
		GrayF32 derivX = new GrayF32(200,200);
		GrayF32 derivY = new GrayF32(200,200);

		DescribePointSift<GrayF32> alg =
				new DescribePointSift<>(4,4,8,1.5,0.5,0.2,GrayF32.class);
		int r = alg.getCanonicalRadius();
		ImageMiscOps.fillRectangle(derivX,5.0f,60,60,2*r,2*r);

		alg.setImageGradient(derivX,derivY);
		alg.descriptor = new TupleDesc_F64(128);
		alg.computeRawDescriptor(60+r, 60+r, 1, 0);

		int numHit = computeInside(alg);
		assertEquals(4*4,numHit);

		alg.descriptor = new TupleDesc_F64(128);
		alg.computeRawDescriptor(60+r, 60+r, 2, 0);
		numHit = computeInside(alg);
		// would be 2x2 if there was no interpolation
		assertEquals(3*3,numHit);

	}

	private int computeInside(DescribePointSift alg) {
		int numHit = 0;
		for (int j = 0; j < 128; j++) {
			if (j % 8 == 0) {
				if(alg.descriptor.value[j] > 0)
					numHit++;
			} else {
				assertEquals(0, alg.descriptor.value[j], 1e-4);
			}
		}
		return numHit;
	}

	/**
	 * Have a constant gradient, which has an easy to understand descriptor, then rotate the feature and see
	 * if the description changes as expected.
	 */
	@Test
	public void computeRawDescriptor_rotation() {
		GrayF32 derivX = new GrayF32(60,55);
		GrayF32 derivY = new GrayF32(60,55);

		ImageMiscOps.fill(derivX,5.0f);
		
		DescribePointSift<GrayF32> alg =
				new DescribePointSift<>(4,4,8,1.5,0.5,0.2,GrayF32.class);
		alg.setImageGradient(derivX,derivY);

		for( int i = 0; i < 8; i++ ) {
			double angle = UtilAngle.bound(i*Math.PI/4);
			alg.descriptor = new TupleDesc_F64(128);
			alg.computeRawDescriptor(20, 21, 1, angle);

			int bin = (int) (UtilAngle.domain2PI(-angle) * 8 / (2 * Math.PI));
			for (int j = 0; j < 128; j++) {
				if (j % 8 == bin) {
					assertTrue(alg.descriptor.value[j] > 0);
				} else {
					assertEquals(0, alg.descriptor.value[j], 1e-4);
				}
			}
		}
		
	}

}