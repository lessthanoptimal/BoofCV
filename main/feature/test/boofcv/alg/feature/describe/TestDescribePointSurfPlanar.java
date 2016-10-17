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
import boofcv.core.image.ConvertImage;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDescribePointSurfPlanar {

	int width = 200;
	int height = 250;

	Random rand = new Random(234);

	/**
	 * Computes the descriptor inside a random image.  Sees if it has the expected results by comparing it to
	 * the single band algorithm which it uses internally.
	 */
	@Test
	public void compareToSingleBand() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,width,height,3);

		GImageMiscOps.addUniform(input,rand,0,200);

		DescribePointSurf<GrayF32> desc = new DescribePointSurf<>(GrayF32.class);
		DescribePointSurfPlanar<GrayF32> alg = new DescribePointSurfPlanar<>(desc,3);

		GrayF32 gray = ConvertImage.average(input,null);

		// input isn't an integral image, but I just want to see if it produces the expected results
		alg.setImage(gray,input);

		for( int i = 0; i < 100; i++ ) {
			double x = rand.nextDouble()*width;
			double y = rand.nextDouble()*height;
			double angle = rand.nextDouble()*Math.PI*2;
			double scale = rand.nextDouble()*10+0.9;

			BrightFeature found = alg.createDescription();
			alg.describe(x,y,angle,scale,found);

			desc.setImage(gray);
			boolean expectedLaplace = desc.computeLaplaceSign((int)(x+0.5),(int)(y+0.5),scale);
			assertEquals(expectedLaplace,found.white);

			BrightFeature expected = desc.createDescription();

			for( int b = 0; b < 3; b++ ) {
				desc.setImage(input.getBand(b));
				desc.describe(x,y,angle,scale,expected);

				// should be off by a constant scale factor since it is normalized across all bands not just one
				double norm = 0;
				for( int j = 0; j < expected.size(); j++ ) {
					double v = found.getDouble(j+b*expected.size());
					norm += v*v;
				}
				norm = Math.sqrt(norm);

				for( int j = 0; j < expected.size(); j++ ) {
					assertEquals(expected.getDouble(j),found.getDouble(j+b*expected.size())/norm,1e-8);
				}
			}
		}
	}
}
