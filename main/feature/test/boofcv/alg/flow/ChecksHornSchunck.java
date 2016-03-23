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

package boofcv.alg.flow;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class ChecksHornSchunck<T extends ImageGray, D extends ImageGray> {
	Class<T> imageType;
	Class<D> derivType;

	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	protected ChecksHornSchunck(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public abstract HornSchunck<T,D> createAlg();

	/**
	 * Manually construct the input so that it has a known and easily understood output
	 */
	@Test
	public void process() {
		HornSchunck<T,D> alg = createAlg();

		T image1 = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T image2 = GeneralizedImageOps.createSingleBand(imageType,width,height);
		ImageFlow output = new ImageFlow(width,height);

		GImageMiscOps.fillRectangle(image1, 100, 10, 0, 20, 30);
		GImageMiscOps.fillRectangle(image2, 100, 11, 0, 20, 30);


		alg.process(image1, image2, output);

		for( int y = 0; y < height-1; y++ ) {
			assertTrue( output.get(9,y).x > 0.9);
			assertTrue( Math.abs(output.get(10,y).y) < 0.1 );
			assertTrue( output.get(10,y).x > 0.9);
			assertTrue( Math.abs(output.get(11,y).y) < 0.1 );
		}
	}

	@Test
	public void computeDerivX() {
		Point[] samples = new Point[8];
		float signs[] = new float[]{1,-1,1,-1,1,-1,1,-1};
		samples[0] = new Point(1,0,0);
		samples[1] = new Point(0,0,0);
		samples[2] = new Point(1,1,0);
		samples[3] = new Point(0,1,0);
		samples[4] = new Point(1,0,1);
		samples[5] = new Point(0,0,1);
		samples[6] = new Point(1,1,1);
		samples[7] = new Point(0,1,1);

		HornSchunck<T,D> alg = createAlg();

		T image1 = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T image2 = GeneralizedImageOps.createSingleBand(imageType,width,height);
		D found = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(image1,rand,0,200);
		GImageMiscOps.fillUniform(image2,rand,0,200);

		alg.computeDerivX(image1,image2,found);
		GrayF32 expected = computeExpected(image1,image2,samples,signs);

		BoofTesting.assertEquals(expected, found, 1);
	}

	@Test
	public void computeDerivY() {
		Point[] samples = new Point[8];
		float signs[] = new float[]{1,-1,1,-1,1,-1,1,-1};
		samples[0] = new Point(0,1,0);
		samples[1] = new Point(0,0,0);
		samples[2] = new Point(1,1,0);
		samples[3] = new Point(1,0,0);
		samples[4] = new Point(0,1,1);
		samples[5] = new Point(0,0,1);
		samples[6] = new Point(1,1,1);
		samples[7] = new Point(1,0,1);

		HornSchunck<T,D> alg = createAlg();

		T image1 = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T image2 = GeneralizedImageOps.createSingleBand(imageType,width,height);
		D found = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(image1,rand,0,200);
		GImageMiscOps.fillUniform(image2,rand,0,200);

		alg.computeDerivY(image1,image2,found);
		GrayF32 expected = computeExpected(image1,image2,samples,signs);

		BoofTesting.assertEquals(expected,found,1);
	}

	@Test
	public void computeDerivT() {
		Point[] samples = new Point[8];
		float signs[] = new float[]{1,-1,1,-1,1,-1,1,-1};
		samples[0] = new Point(0,0,1);
		samples[1] = new Point(0,0,0);
		samples[2] = new Point(1,0,1);
		samples[3] = new Point(1,0,0);
		samples[4] = new Point(0,1,1);
		samples[5] = new Point(0,1,0);
		samples[6] = new Point(1,1,1);
		samples[7] = new Point(1,1,0);

		HornSchunck<T,D> alg = createAlg();

		T image1 = GeneralizedImageOps.createSingleBand(imageType, width, height);
		T image2 = GeneralizedImageOps.createSingleBand(imageType,width,height);
		D found = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(image1,rand,0,200);
		GImageMiscOps.fillUniform(image2,rand,0,200);

		alg.computeDerivT(image1,image2,found);
		GrayF32 expected = computeExpected(image1,image2,samples,signs);

		BoofTesting.assertEquals(expected,found,1);
	}

	private GrayF32 computeExpected(ImageGray image1, ImageGray image2 ,
									Point[] samples , float signs[] ) {
		GrayF32 ret = new GrayF32(width,height);

		for (int y = 0; y < image1.height; y++) {
			for (int x = 0; x < image1.width; x++) {
				float total = 0;

				for( int i = 0; i < signs.length; i++ ) {
					Point p = samples[i];
					float s = signs[i];

					if( p.k == 0 ) {
						total += s*safeGet(image1,x+p.x,y+p.y);
					} else {
						total += s*safeGet(image2,x+p.x,y+p.y);
					}
				}

				total *= 0.25;
				ret.set(x,y,total);
			}
		}

		return ret;
	}

	private float safeGet(ImageGray image , int x , int y ) {
		if( x < 0 ) x = 0;
		if( x >= image.width) x = image.width-1;
		if( y < 0 ) y = 0;
		if( y >= image.height) y = image.height-1;

		return (float) GeneralizedImageOps.get(image, x, y);
	}

	private static class Point {
		int x,y,k;

		private Point(int x, int y, int k) {
			this.x = x;
			this.y = y;
			this.k = k;
		}
	}
}
