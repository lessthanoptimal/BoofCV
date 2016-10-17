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

package boofcv.abst.filter.interpolate;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestInterpolatePixel_PL_using_SB {

	Random rand = new Random(234);
	int width = 30;
	int height = 40;

	@Test
	public void compareToIndividual() {
		GrayF32 image0 = new GrayF32(width,height);
		GrayF32 image1 = new GrayF32(width,height);

		ImageMiscOps.fillUniform(image0,rand,0,100);
		ImageMiscOps.fillUniform(image1,rand,0,100);

		InterpolatePixelS<GrayF32> interpA =
				FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		InterpolatePixelS<GrayF32> interpB =
				FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);

		InterpolatePixelMB<Planar<GrayF32>> alg = new InterpolatePixel_PL_using_SB<>(interpB);

		Planar<GrayF32> mb = new Planar<>(GrayF32.class,width,height,2);
		mb.bands[0] = image0;
		mb.bands[1] = image1;

		alg.setImage(mb);

		float vals[] = new float[2];
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				float xx = (rand.nextFloat()-0.5f) + x;
				float yy = (rand.nextFloat()-0.5f) + y;

				if( xx < 0 ) xx = 0; else if( xx > width-1) xx = width-1;
				if( yy < 0 ) yy = 0; else if( yy > height-1) yy = height-1;

				alg.get(xx,yy,vals);

				interpA.setImage(image0);
				float expected0 = interpA.get(xx,yy);
				interpA.setImage(image1);
				float expected1 = interpA.get(xx,yy);

				assertEquals(expected0,vals[0],1e-4);
				assertEquals(expected1,vals[1],1e-4);
			}
		}

	}

}
