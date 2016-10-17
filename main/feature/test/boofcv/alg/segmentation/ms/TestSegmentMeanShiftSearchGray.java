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

package boofcv.alg.segmentation.ms;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* @author Peter Abeles
*/
public class TestSegmentMeanShiftSearchGray {

	Random rand = new Random(234);

	InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);

	/**
	 * Process a random image and do a basic sanity check on the output
	 */
	@Test
	public void simpleTest() {
		GrayF32 image = new GrayF32(20,25);

		ImageMiscOps.fillUniform(image, rand, 0, 256);

		SegmentMeanShiftSearchGray<GrayF32> alg =
				new SegmentMeanShiftSearchGray<>(30,0.05f,interp,2,2,100, false);

		alg.process(image);

		FastQueue<Point2D_I32> locations = alg.getModeLocation();
		GrowQueue_I32 counts = alg.getRegionMemberCount();
		GrayS32 peaks = alg.getPixelToRegion();
		FastQueue<float[]> values = alg.getModeColor();

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

				// can't test the value because its floating point location which is interpolated using the kernel
				// and the location is lost
//				assertEquals(x+" "+y,computeValue(peakX,peakY,image),value,50);

				assertTrue( counts.get(peak) > 0 );
			}
		}
	}

	@Test
	public void findPeak_inside() {
		GrayF32 image = new GrayF32(20,25);

		ImageMiscOps.fillRectangle(image, 20, 4, 2, 5, 5);

		// works better with this example when its uniform
		SegmentMeanShiftSearchGray<GrayF32> alg =
				new SegmentMeanShiftSearchGray<>(30,0.05f,interp,2,2,3, false);

		interp.setImage(image);
		alg.image = image;
		alg.findPeak(4,2,20);

		assertEquals( 6 , alg.modeX, 0.5f );
		assertEquals( 4 , alg.modeY, 0.5f );
	}

	@Test
	public void findPeak_border() {
		findPeak_border(2, 2, 0, 0);
		findPeak_border(17, 22, 19, 24);
	}

	private void findPeak_border(int cx, int cy, int startX, int startY) {
		GrayF32 image = new GrayF32(20,25);

		ImageMiscOps.fillRectangle(image, 20, cx - 2, cy - 2, 5, 5);

		SegmentMeanShiftSearchGray<GrayF32> alg =
				new SegmentMeanShiftSearchGray<>(30,0.05f,interp,2,2,100, false);

		interp.setImage(image);
		alg.image = image;
		alg.findPeak(startX,startY,20);

		assertEquals( cx , alg.modeX, 0.5f );
		assertEquals( cy , alg.modeY, 0.5f );
	}
}
