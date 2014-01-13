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

package boofcv.alg.segmentation;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.weights.*;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShiftGray {

	Random rand = new Random(234);

	InterpolatePixelS<ImageFloat32> interp = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
	// use a gaussian distribution by default so that the indexes matter
	WeightPixel_F32 weightSpacial = new WeightPixelGaussian_F32();
	WeightDistance_F32 weightDist = new WeightDistanceUniform_F32(200);

	/**
	 * Process a random image and do a basic sanity check on the output
	 */
	@Test
	public void simpleTest() {
		ImageFloat32 image = new ImageFloat32(20,25);

		ImageMiscOps.fillUniform(image, rand, 0, 256);

		SegmentMeanShiftGray<ImageFloat32> alg =
				new SegmentMeanShiftGray<ImageFloat32>(30,0.05f,interp,weightSpacial, weightDist);
		alg.setRadius(2);

		alg.process(image);

		GrowQueue_I32 locations = alg.getPeakLocation();
		GrowQueue_I32 counts = alg.getPeakMemberCount();
		ImageSInt32 peaks = alg.getPeakToIndex();
		GrowQueue_F32 values = alg.getPeakValue();

		// there should be a fair number of local peaks due to the image being random
		assertTrue( locations.size > 20 );
		// all the lists should be the same size
		assertEquals(locations.size,counts.size);
		assertEquals(locations.size,values.size);

		// total members should equal the number of pixels
		int totalMembers = 0;
		for( int i = 0; i < counts.size; i++ ) {
			totalMembers += counts.get(i);
		}
		assertEquals(20*25,totalMembers);

		// see if the peak to index image is set up correctly and that all the peaks make sense
		for( int y = 0; y < peaks.height; y++ ) {
			for( int x = 0; x < peaks.width; x++ ) {
				int peak = peaks.get(x,y);
				int locationIndex = locations.get(peak);
				float value = values.get(peak);

				int peakX = locationIndex%20;
				int peakY = locationIndex/20;

				assertEquals(x+" "+y,image.get(peakX,peakY),value,1e-4);

				assertTrue( counts.get(peak) > 0 );
			}
		}

	}

	@Test
	public void findPeak_inside() {
		ImageFloat32 image = new ImageFloat32(20,25);

		ImageMiscOps.fillRectangle(image, 20, 4, 2, 5, 5);

		// works better with this example when its uniform
		WeightPixel_F32 weightSpacial = new WeightPixelUniform_F32();
		SegmentMeanShiftGray<ImageFloat32> alg =
				new SegmentMeanShiftGray<ImageFloat32>(30,0.05f,interp,weightSpacial, weightDist);
		alg.setRadius(2);

		interp.setImage(image);
		alg.image = image;
		int index = alg.findPeak(4,2,20);

		assertEquals( 6 , index%20 );
		assertEquals( 4 , index/20 );
	}

	@Test
	public void findPeak_border() {
		findPeak_border(2, 2, 0, 0);
		findPeak_border(17, 22, 19, 24);
	}

	private void findPeak_border(int cx, int cy, int startX, int startY) {
		ImageFloat32 image = new ImageFloat32(20,25);

		ImageMiscOps.fillRectangle(image, 20, cx - 2, cy - 2, 5, 5);

		SegmentMeanShiftGray<ImageFloat32> alg =
				new SegmentMeanShiftGray<ImageFloat32>(30,0.05f,interp,weightSpacial, weightDist);
		alg.setRadius(2);

		interp.setImage(image);
		alg.image = image;
		int index = alg.findPeak(startX,startY,20);

		assertEquals( cx , index%20 );
		assertEquals(cy, index / 20);
	}

	@Test
	public void computeGray_inside() {
		ImageFloat32 image = new ImageFloat32(30,40);

		ImageMiscOps.fillUniform(image,rand,0,256);

		computeGray_border(image, 10, 12);
	}

	@Test
	public void computeGray_border() {
		ImageFloat32 image = new ImageFloat32(30,40);

		ImageMiscOps.fillUniform(image,rand,0,256);

		computeGray_border(image, 0, 0);
		computeGray_border(image, 29, 39);
	}

	private void computeGray_border(ImageFloat32 image, int cx, int cy) {
		SegmentMeanShiftGray<ImageFloat32> alg =
				new SegmentMeanShiftGray<ImageFloat32>(30,0.05f,interp,weightSpacial, weightDist);
		alg.setRadius(2);

		interp.setImage(image);
		alg.image = image;
		float found = alg.computeGray(cx,cy);

		float expected = 0;
		float area = 0;
		for( int i = -2; i <= 2; i++ ) {
			for( int j = -2; j <= 2; j++ ) {
				if( image.isInBounds(cx+j,cy+i)) {
					float w = weightSpacial.weight(j,i);
					expected += w*image.get(cx+j,cy+i);
					area += w;
				}
			}
		}
		expected /= area;

		assertEquals(expected,found,1e-4f);
	}

}
