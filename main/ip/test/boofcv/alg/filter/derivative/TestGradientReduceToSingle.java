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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGradientReduceToSingle {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;
	int numbands = 3;

	@Test
	public void maxf_plf32_f32() {
		Planar<GrayF32> inX = new Planar<>(GrayF32.class,width,height,numbands);
		Planar<GrayF32> inY = new Planar<>(GrayF32.class,width,height,numbands);
		GrayF32 outX = new GrayF32(width,height);
		GrayF32 outY = new GrayF32(width,height);

		GImageMiscOps.fillUniform(inX,rand,0,100);
		GImageMiscOps.fillUniform(inY,rand,0,100);

		GradientReduceToSingle.maxf(inX,inY,outX,outY);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float maxNorm = 0;
				float maxValueX = -Float.MAX_VALUE;
				float maxValueY = -Float.MAX_VALUE;

				for (int i = 0; i < numbands; i++) {
					float dx = inX.getBand(i).get(x,y);
					float dy = inY.getBand(i).get(x,y);

					float r = dx*dx + dy*dy;
					if( r > maxNorm ) {
						maxNorm = r;
						maxValueX = dx;
						maxValueY = dy;
					}
				}

				assertEquals(maxValueX, outX.get(x,y), 1e-4f);
				assertEquals(maxValueY, outY.get(x,y), 1e-4f);
			}
		}
	}

	@Test
	public void maxf_plfu8_u8() {
		Planar<GrayU8> inX = new Planar<>(GrayU8.class,width,height,numbands);
		Planar<GrayU8> inY = new Planar<>(GrayU8.class,width,height,numbands);
		GrayU8 outX = new GrayU8(width,height);
		GrayU8 outY = new GrayU8(width,height);

		GImageMiscOps.fillUniform(inX,rand,0,100);
		GImageMiscOps.fillUniform(inY,rand,0,100);

		GradientReduceToSingle.maxf(inX,inY,outX,outY);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int maxNorm = 0;
				int maxValueX = -Integer.MAX_VALUE;
				int maxValueY = -Integer.MAX_VALUE;

				for (int i = 0; i < numbands; i++) {
					int dx = inX.getBand(i).get(x,y);
					int dy = inY.getBand(i).get(x,y);

					int r = dx*dx + dy*dy;
					if( r > maxNorm ) {
						maxNorm = r;
						maxValueX = dx;
						maxValueY = dy;
					}
				}

				assertEquals(maxValueX, outX.get(x,y), 1e-4f);
				assertEquals(maxValueY, outY.get(x,y), 1e-4f);
			}
		}
	}
}