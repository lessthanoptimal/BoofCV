/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.util.UtilDouble;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrackerMeanShiftComaniciu2003 extends BoofStandardJUnit {

	@Test void track() {
		InterpolatePixelS<GrayF32> interpSB = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		InterpolatePixelMB<Planar<GrayF32>> interpolate = FactoryInterpolation.createPixelPL(interpSB);
		var calcHistogram = new LocalWeightedHistogramRotRect<>(30,3,10,3,255,interpolate);
		var alg = new TrackerMeanShiftComaniciu2003<>(false,100,1e-8f,0.0f,0.0f,0.1f,calcHistogram);

		Planar<GrayF32> image = new Planar<>(GrayF32.class,100,150,3);

		// odd width and height so samples land on pixels
		render(image,50,40,21,31);

		RectangleRotate_F32 found = new RectangleRotate_F32(50,40,21,31,0);
		alg.initialize(image,found);

		// test no change
		alg.track(image);
		check(alg.getRegion(),50,40,21,31,0);

		// test translation
		render(image,55,34,21,31);
		alg.track(image);
		check(alg.getRegion(),55,34,21,31,0);

		// test scale
		render(image,55,34,23,34);
		alg.track(image);

		assertEquals(alg.getRegion().cx,55,1f);
		assertEquals(alg.getRegion().cy,34,1f);
		assertEquals(alg.getRegion().width,23,1);
		assertEquals(alg.getRegion().height,34,1);
	}

	@Test void updateLocation() {
		InterpolatePixelS<GrayF32> interpSB = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
		InterpolatePixelMB<Planar<GrayF32>> interpolate = FactoryInterpolation.createPixelPL(interpSB);
		var calcHistogram = new LocalWeightedHistogramRotRect<>(30,3,10,3,255,interpolate);
		var alg = new TrackerMeanShiftComaniciu2003<>(false,100,1e-8f,0.1f,0.0f,0.1f,calcHistogram);

		Planar<GrayF32> image = new Planar<>(GrayF32.class,100,150,3);

		// odd width and height so samples land on pixels
		render(image,50,40,21,31);

		RectangleRotate_F32 found = new RectangleRotate_F32(50,40,21,31,0);
		alg.initialize(image,found);

		// test no change
		alg.updateLocation(image,found);
		check(found,50,40,21,31,0);

		// test translation
		render(image,55,34,21,31);
		alg.updateLocation(image,found);

		check(found,55,34,21,31,0);
	}

	private void check( RectangleRotate_F32 found , float cx, float cy, float width, float height, float theta ) {
		float tol = 0.5f;

		assertEquals(found.cx,cx,tol);
		assertEquals(found.cy,cy,tol);
		assertEquals(found.width,width,tol);
		assertEquals(found.height,height,tol);
		assertEquals(found.theta,theta,tol);
	}

	@Test void distanceHistogram() {
		LocalWeightedHistogramRotRect calcHist = new LocalWeightedHistogramRotRect(10,3,5,3,255,null);

		TrackerMeanShiftComaniciu2003 alg = new TrackerMeanShiftComaniciu2003(true,100,1e-4f,0.1f,0.0f,0.1f,calcHist);

		float[] histogramA = new float[ calcHist.getHistogram().length ];
		float[] histogramB = new float[ calcHist.getHistogram().length ];

		// score for identical histograms
		for( int i = 0; i < histogramA.length; i++ ) {
			histogramA[i] = histogramB[i] = rand.nextFloat();
		}
		UtilDouble.normalize(histogramA);
		UtilDouble.normalize(histogramB);

		double foundIdentical = alg.distanceHistogram(histogramA,histogramB);
		assertEquals(0,foundIdentical,1e-3);

		// make the histograms very different
		for( int i = 0; i < histogramA.length; i++ ) {
			histogramA[i] = rand.nextFloat();
		}
		UtilDouble.normalize(histogramA);

		double foundDifferent = alg.distanceHistogram(histogramA,histogramB);

		assertTrue(foundDifferent <= 1.0 );
		assertTrue(foundDifferent > 0.05);
	}

	private void render(Planar<GrayF32> image , int cx , int cy , int w , int h ) {
		GImageMiscOps.fill(image,0);

		int tl_x = cx-w/2;
		int tl_y = cy-h/2;

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				if( x > w/4 && x < 3*w/4 ) {
					image.getBand(0).set(x+tl_x,y+tl_y,100);
					image.getBand(1).set(x+tl_x,y+tl_y,200);
					image.getBand(2).set(x+tl_x,y+tl_y,150);
				} else {
					image.getBand(0).set(x+tl_x,y+tl_y,200);
					image.getBand(1).set(x+tl_x,y+tl_y,76);
					image.getBand(2).set(x+tl_x,y+tl_y,40);
				}
			}
		}
	}

}
