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

package boofcv.alg.feature.dense;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDescribeDenseSiftAlg {

	Random rand = new Random(234);
	int width = 50, height = 40;

	/**
	 * Checks the adjustment done to the sample period and to see if the descriptions are computed
	 * at the correct coordinate
	 */
	@Test
	public void process() {
		GrayF32 derivX = new GrayF32(100,102);
		GrayF32 derivY = new GrayF32(100,102);

		GImageMiscOps.fillUniform(derivX,rand,0,200);
		GImageMiscOps.fillUniform(derivY,rand,0,200);

		process(derivX, derivY);
		BoofTesting.checkSubImage(this,"process",true,derivX,derivY);
	}

	public void process(GrayF32 derivX, GrayF32 derivY) {
		DescribeDenseSiftAlg<GrayF32> alg = new DescribeDenseSiftAlg<>(4,4,8,0.5,0.2,10,10,GrayF32.class);

		alg.setImageGradient(derivX,derivY);

		alg.process();

		List<TupleDesc_F64> list = alg.getDescriptors().toList();

		int r = alg.getCanonicalRadius();

		int cols = (100-2*r)/10;
		int rows = (102-2*r)/10;

		assertEquals(cols*rows,list.size());

		int w = derivX.width-2*r;
		int h = derivX.height-2*r;

		TupleDesc_F64 expected = new TupleDesc_F64(128);

		int i = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++) {
				int x = col*w/(cols-1) + r;
				int y = row*h/(rows-1) + r;

				TupleDesc_F64 found = list.get(i);
				alg.computeDescriptor(x,y,expected);

				for (int j = 0; j < 128; j++) {
					assertEquals(expected.value[j],found.value[j],1e-8);
				}
			}
		}
	}

	@Test
	public void precomputeAngles() {
		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);

		GImageMiscOps.fillUniform(derivX,rand,0,200);
		GImageMiscOps.fillUniform(derivY,rand,0,200);

		DescribeDenseSiftAlg<GrayF32> alg = new DescribeDenseSiftAlg<>(4,4,8,0.5,0.2,10,10,GrayF32.class);

		alg.setImageGradient(derivX,derivY);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float dx = derivX.get(x,y);
				float dy = derivY.get(x,y);

				double expectedAngle = UtilAngle.domain2PI(Math.atan2(dy,dx));
				float expectedMagnitude = (float)Math.sqrt(dx*dx + dy*dy);

				assertEquals(expectedAngle,alg.savedAngle.get(x,y),1e-8);
				assertEquals(expectedMagnitude,alg.savedMagnitude.get(x,y),1e-4f);
			}
		}
	}

	/**
	 * Compute to the general descriptor algorithm.  They should produce the same results
	 */
	@Test
	public void computeDescriptor() {
		GrayF32 derivX = new GrayF32(100,102);
		GrayF32 derivY = new GrayF32(100,102);

		GImageMiscOps.fillUniform(derivX,rand,0,200);
		GImageMiscOps.fillUniform(derivY,rand,0,200);

		DescribeDenseSiftAlg<GrayF32> alg = new DescribeDenseSiftAlg<>(4,4,8,0.5,0.2,10,10,GrayF32.class);
		DescribePointSift<GrayF32> algTest = new DescribePointSift<>(4,4,8,1,0.5,0.2,GrayF32.class);

		alg.setImageGradient(derivX,derivY);
		algTest.setImageGradient(derivX,derivY);

		List<Point2D_I32> samplePoints = new ArrayList<>();
		samplePoints.add( new Point2D_I32(30,35));
		samplePoints.add( new Point2D_I32(45,10));
		samplePoints.add( new Point2D_I32(60,12));
		samplePoints.add( new Point2D_I32(50,50));

		TupleDesc_F64 found = new TupleDesc_F64(128);
		TupleDesc_F64 expected = new TupleDesc_F64(128);

		for( Point2D_I32 p : samplePoints ) {
			alg.computeDescriptor(p.x,p.y,found);
			algTest.process(p.x,p.y,1,0,expected);

			for (int i = 0; i < 128; i++) {
				assertEquals(expected.value[i],found.value[i],1e-8);
			}
		}

	}
}