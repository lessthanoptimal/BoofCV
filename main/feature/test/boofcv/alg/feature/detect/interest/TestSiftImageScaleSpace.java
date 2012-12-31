/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSiftImageScaleSpace {

	Random rand = new Random(234);

	@Test
	public void computeScaleSigma() {

		SiftImageScaleSpace alg = new SiftImageScaleSpace(1.6f, 5, 2, false);

		assertEquals(1.6, alg.computeScaleSigma(0, 0), 1e-4);
		assertEquals( 3.2 , alg.computeScaleSigma(0,1) , 1e-4 );
		assertEquals(4.8, alg.computeScaleSigma(0, 2), 1e-4);

		// compute total gaussian blur from previous set taken at level 2
		double prev = 2*1.6;
		// Each level still has 1.6, but at 1/2 the resolution
		double next1 = Math.sqrt( prev*prev + 4*1.6*1.6 );
		double next2 = Math.sqrt( prev*prev + 4*3.2*3.2 );

		assertEquals( next1 , alg.computeScaleSigma(1,0) , 1e-4);
		assertEquals( next2 , alg.computeScaleSigma(1,1) , 1e-4 );
	}

	@Test
	public void downSample() {
		checkDownSample(20,30);
		checkDownSample(19, 29);
	}

	private void checkDownSample( int w , int h ) {
		ImageFloat32 input = new ImageFloat32(w,h);
		ImageFloat32 output = new ImageFloat32(w/2,h/2);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		SiftImageScaleSpace.downSample(input, output);

		for( int i = 0; i < output.height; i++ ) {
			for( int j = 0; j < output.width; j++ ) {
				assertTrue(input.get(j * 2+1, i * 2+1) == output.get(j, i));
			}
		}
	}

	@Test
	public void upSample() {
		checUpSample(20, 30);
		checUpSample(19, 29);
	}

	private void checUpSample( int w , int h ) {
		ImageFloat32 input = new ImageFloat32(w,h);
		ImageFloat32 output = new ImageFloat32(w*2,h*2);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		SiftImageScaleSpace.upSample(input, output);

		for( int i = 0; i < output.height; i++ ) {
			for( int j = 0; j < output.width; j++ ) {
				assertTrue(input.get(j/2, i/2) == output.get(j, i));
			}
		}
	}

	@Test
	public void scaleToImageIndex() {
		SiftImageScaleSpace alg = new SiftImageScaleSpace(1.6f, 5, 2, false);

		// try easy cases exactly on the nominal sigma
		assertEquals(0,alg.scaleToImageIndex(alg.computeScaleSigma(0, 0)));
		assertEquals(1,alg.scaleToImageIndex(alg.computeScaleSigma(0, 1)));
		assertEquals(2,alg.scaleToImageIndex(alg.computeScaleSigma(0, 2)));
		assertEquals(5,alg.scaleToImageIndex(alg.computeScaleSigma(1, 0)));
		assertEquals(6,alg.scaleToImageIndex(alg.computeScaleSigma(1, 1)));
		assertEquals(9,alg.scaleToImageIndex(alg.computeScaleSigma(1, 4)));

		// try cases slightly off from nominal
		assertEquals(0, alg.scaleToImageIndex(alg.computeScaleSigma(0, 0) - 0.01));
		assertEquals(0,alg.scaleToImageIndex(alg.computeScaleSigma(0, 0)+0.01));
		assertEquals(9,alg.scaleToImageIndex(alg.computeScaleSigma(1, 4)-0.01));
		assertEquals(9,alg.scaleToImageIndex(alg.computeScaleSigma(1, 4)+0.01));

		// try the extreme ends, which should round in the wrong direction
		assertEquals(0, alg.scaleToImageIndex(0));
		assertEquals(9,alg.scaleToImageIndex(alg.computeScaleSigma(1, 4)+1.6*2));
	}

	@Test
	public void imageIndexToPixelScale() {
		SiftImageScaleSpace alg = new SiftImageScaleSpace(1.6f, 5, 2, false);

		assertEquals(1.0,alg.imageIndexToPixelScale(0),1e-8);
		assertEquals(1.0,alg.imageIndexToPixelScale(1),1e-8);
		assertEquals(1.0,alg.imageIndexToPixelScale(4),1e-8);
		assertEquals(2.0,alg.imageIndexToPixelScale(5),1e-8);
		assertEquals(2.0,alg.imageIndexToPixelScale(6),1e-8);
		assertEquals(2.0,alg.imageIndexToPixelScale(9),1e-8);
	}

	/**
	 * The number of octaves is too large for input image and should stop processing before it gets too small
	 */
	@Test
	public void smallImages() {
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f, 5, 4, false);

		ImageFloat32 input = new ImageFloat32(10,15);
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		ss.constructPyramid(input);
		ss.computeFeatureIntensity();
		ss.computeDerivatives();

		assertEquals(2, ss.actualOctaves);

		// images in rest of the octaves should be zero
		int index = 3*5;
		for( ; index < ss.scale.length; index++ ) {
			ImageFloat32 s = ss.scale[index];
			ImageFloat32 gx = ss.derivX[index];
			ImageFloat32 gy = ss.derivY[index];
			assertTrue(ImageStatistics.sum(s)==0);
			assertTrue(ImageStatistics.sum(gx)==0);
			assertTrue(ImageStatistics.sum(gy)==0);
		}

		index = 3*4;
		for( ; index < ss.dog.length; index++ ) {
			ImageFloat32 dog = ss.dog[index];
			assertTrue(ImageStatistics.sum(dog)==0);
		}
	}

	/**
	 * Process two images, one is a sub-image of the first.  See if it produces the same results
	 */
	@Test
	public void checkSubImage() {
		SiftImageScaleSpace ss1 = new SiftImageScaleSpace(1.6f, 5, 4, false);
		SiftImageScaleSpace ss2 = new SiftImageScaleSpace(1.6f, 5, 4, false);

		ImageFloat32 input = new ImageFloat32(60,70);
		GImageMiscOps.fillUniform(input, rand, 0, 100);
		ImageFloat32 sub = BoofTesting.createSubImageOf(input);

		ss1.constructPyramid(input);
		ss2.constructPyramid(sub);

		for( int index = 0; index < ss1.scale.length; index++ ) {
			ImageFloat32 s1 = ss1.scale[index];
			ImageFloat32 s2 = ss2.scale[index];

			float sum1 = ImageStatistics.sum(s1);
			float sum2 = ImageStatistics.sum(s2);

			assertTrue(sum1 != 0);
			assertEquals(sum1,sum2,1e-6);
		}
	}
}
