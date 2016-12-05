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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
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
public class TestSegmentMeanShiftSearchColor {

	Random rand = new Random(234);

	ImageType<Planar<GrayF32>> imageType = ImageType.pl(2,GrayF32.class);
	InterpolatePixelMB<Planar<GrayF32>> interp =
			FactoryInterpolation.createPixelMB(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,imageType);

	/**
	 * Process a random image and do a basic sanity check on the output
	 */
	@Test
	public void simpleTest() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,20,25,2);

		GImageMiscOps.fillUniform(image, rand, 0, 256);

		SegmentMeanShiftSearchColor<Planar<GrayF32>> alg =
				new SegmentMeanShiftSearchColor<>
						(30, 0.05f, interp, 2, 2, 200, false, imageType);

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
		Planar<GrayF32> image = new Planar<>(GrayF32.class,20,25,2);

		GImageMiscOps.fillRectangle(image, 20, 4, 2, 5, 5);

		// works better with this example when its uniform
		SegmentMeanShiftSearchColor<Planar<GrayF32>> alg =
				new SegmentMeanShiftSearchColor<>
						(30, 0.05f, interp, 2, 2, 3, false, imageType);

		interp.setImage(image);
		alg.image = image;
		alg.findPeak(4,2,new float[]{20,20});

		assertEquals( 6 , alg.modeX, 0.5f );
		assertEquals(4, alg.modeY, 0.5f);
	}

	@Test
	public void findPeak_border() {
		findPeak_border(2, 2, 0, 0);
		findPeak_border(17, 22, 19, 24);
	}

	private void findPeak_border(int cx, int cy, int startX, int startY) {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,20,25,2);

		GImageMiscOps.fillRectangle(image, 20, cx - 2, cy - 2, 5, 5);

		SegmentMeanShiftSearchColor<Planar<GrayF32>> alg =
				new SegmentMeanShiftSearchColor<>
						(30, 0.05f, interp, 2, 2, 200, false, imageType);

		interp.setImage(image);
		alg.image = image;
		alg.findPeak(startX,startY,new float[]{20,20});

		assertEquals( cx , alg.modeX, 0.5f );
		assertEquals(cy, alg.modeY, 0.5f);
	}

	/**
	 * Identical results should be produced for gray scale algorithm and color algorithm when there is a single band
	 */
	@Test
	public void compareToGray() {
		compareToGray(false);
		compareToGray(true);
	}

	public void compareToGray( boolean fast ) {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,20,25,1);

		GImageMiscOps.fillUniform(image, rand, 0, 256);

		ImageType<Planar<GrayF32>> imageType = ImageType.pl(1,GrayF32.class);
		InterpolatePixelMB<Planar<GrayF32>> interpMB =
				FactoryInterpolation.createPixelMB(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,imageType);
		InterpolatePixelS<GrayF32> interpSB = FactoryInterpolation.bilinearPixelS(
				GrayF32.class, BorderType.EXTENDED);

		SegmentMeanShiftSearchColor<Planar<GrayF32>> algMB =
				new SegmentMeanShiftSearchColor<>
						(30, 0.05f, interpMB, 2, 2, 200, fast, imageType);

		SegmentMeanShiftSearchGray<GrayF32> algSB =
				new SegmentMeanShiftSearchGray<>(30,0.05f,interpSB,2,2,200,fast);

		algMB.process(image);
		algSB.process(image.getBand(0));

		// there should be a fair number of local peaks due to the image being random
		assertTrue( algMB.getModeLocation().size > 20 );

		assertEquals(algMB.getModeColor().size, algSB.getModeColor().size);
		assertEquals(algMB.getModeLocation().size,algSB.getModeLocation().size);
		assertEquals(algMB.getRegionMemberCount().size,algSB.getRegionMemberCount().size);

		for( int i = 0; i < algMB.getModeColor().size; i++ ) {
			assertEquals(algMB.getModeColor().get(i)[0],algSB.getModeColor().get(i)[0],1e-4f);
			assertEquals(algMB.getModeLocation().get(i).x,algSB.getModeLocation().get(i).x,1e-4f);
			assertEquals(algMB.getModeLocation().get(i).y,algSB.getModeLocation().get(i).y,1e-4f);
			assertEquals(algMB.getRegionMemberCount().get(i),algSB.getRegionMemberCount().get(i));
		}

		GrayS32 segmentMB = algMB.getPixelToRegion();
		GrayS32 segmentSB = algSB.getPixelToRegion();

		for( int y = 0; y < segmentMB.height; y++ ) {
			for( int x = 0; x < segmentMB.width; x++ ) {
				assertEquals(segmentMB.get(x,y),segmentSB.get(x,y));
			}
		}
	}
}
