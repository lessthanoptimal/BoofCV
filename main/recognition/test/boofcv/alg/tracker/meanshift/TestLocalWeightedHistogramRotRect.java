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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLocalWeightedHistogramRotRect {

	Random rand = new Random(234);

	/**
	 * Crudely checks to see that the center has the most weight
	 */
	@Test
	public void computeWeights() {

		int w = 9;

		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(w,3,12,3,255,null);

		float maxW = alg.weights[w*6 + 6];

		assertTrue(alg.weights[0] < maxW);
		assertTrue(alg.weights[w-1] < maxW);
		assertTrue(alg.weights[(w-1)*w + w-1] < maxW);
		assertTrue(alg.weights[(w-1)*w] < maxW);
	}

	@Test
	public void createSamplePoints() {
		int w = 9;

		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(w,3,12,3,255,null);

		int i = 0;
		for( int y = 0; y < w; y++ ) {
			for( int x = 0; x < w; x++ , i++ ) {
				Point2D_F32 p = (Point2D_F32)alg.samplePts.get(i);
				float expectedX = (x/(float)(w-1))-0.5f;
				float expectedY = (y/(float)(w-1))-0.5f;

				assertEquals(expectedX,p.x,1e-4f);
				assertEquals(expectedY,p.y,1e-4f);
			}
		}
	}

	@Test
	public void computeHistogram() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,40,50,3);
		InterpolatePixelMB interp = FactoryInterpolation.createPixelPL(FactoryInterpolation.bilinearPixelS(
				GrayF32.class, BorderType.EXTENDED));
		GImageMiscOps.fillUniform(image,rand,0,100);
		interp.setImage(image);

		RectangleRotate_F32 rect = new RectangleRotate_F32(20,25,10,15,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,interp);

		alg.computeHistogram(image, rect);

		float hist[] = alg.getHistogram().clone();
		int histIndex[] = alg.getSampleHistIndex().clone();

		// crude sanity check
		int numNotZero = 0;
		for( int i = 0; i < hist.length; i++ )
			if( hist[i] != 0 ) numNotZero++;
		assertTrue(numNotZero > 0);
		for( int i = 0; i < histIndex.length; i++ )
			assertTrue(histIndex[i] != 0);

		// should produce the same answer after a second call
		alg.computeHistogram(image,rect);

		for( int i = 0; i < hist.length; i++ )
			assertEquals(hist[i], alg.getHistogram()[i], 1e-4);
		for( int i = 0; i < histIndex.length; i++ )
			assertEquals(histIndex[i], alg.getSampleHistIndex()[i], 1e-4);
	}

	/**
	 * When given a region entirely inside, both inside and outside should produce identical solutions
	 */
	@Test
	public void computeHistogramBorder_compare() {
		Planar<GrayF32> image = new Planar<>(GrayF32.class,40,50,3);
		InterpolatePixelMB interp = FactoryInterpolation.createPixelPL(FactoryInterpolation.bilinearPixelS(
				GrayF32.class, BorderType.EXTENDED));
		GImageMiscOps.fillUniform(image,rand,0,100);
		interp.setImage(image);

		RectangleRotate_F32 rect = new RectangleRotate_F32(20,25,10,15,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,interp);

		alg.computeHistogramBorder(image,rect);

		int[] insideHistIndex = alg.sampleHistIndex.clone();
		float[] insideHist = alg.histogram.clone();

		alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,interp);

		alg.computeHistogramInside(rect);

		for( int i = 0; i < insideHist.length; i++ ) {
			assertEquals(insideHist[i],alg.histogram[i],1e-4f);
		}
		for( int i = 0; i < insideHistIndex.length; i++ ) {
			assertEquals(insideHistIndex[i],alg.sampleHistIndex[i],1e-4f);
		}
	}

	/**
	 * Make sure it handles pixels outside the image correctly
	 */
	@Test
	public void computeHistogramBorder_outside() {
		int numSamples = 10;

		InterleavedF32 image = new InterleavedF32(40,50,3);
		DummyInterpolate interp = new DummyInterpolate();
		RectangleRotate_F32 rect = new RectangleRotate_F32(4,5,10,20,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(numSamples,3,12,3,255,interp);

	    alg.c = 1; alg.s = 0;

		alg.computeHistogramBorder(image,rect);

		int numInside = 0;
		int i = 0;
		for( int y = 0; y < numSamples; y++ ) {
			for( int x = 0; x < numSamples; x++ , i++ ) {
				alg.squareToImageSample((float)x/(numSamples-1)-0.5f,(float)y/(numSamples-1)-0.5f,rect);

				boolean inside = alg.imageX >= 0 && alg.imageX < 40 && alg.imageY >= 0 && alg.imageY < 50;

				if( inside ) {
					numInside++;
					assertTrue(alg.sampleHistIndex[i] >= 0 );
					assertTrue(alg.histogram[alg.sampleHistIndex[i]] > 0 );
				} else {
					assertTrue(alg.sampleHistIndex[i] == -1 );
				}
			}
		}

		assertTrue(numInside != numSamples*numSamples);

	}

	@Test
	public void computeHistogramBin() {
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,null);

		float div = alg.maxPixelValue/12;

		assertEquals(0,alg.computeHistogramBin(new float[]{0,0,0}));
		assertEquals(1+2*12+3*144,alg.computeHistogramBin(new float[]{1*div,2*div,3*div}));

	}

	@Test
	public void isInFastBounds() {
		DummyInterpolate interp = new DummyInterpolate();
		RectangleRotate_F32 rect = new RectangleRotate_F32(4,5,10,20,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,interp);

		alg.c = 1; alg.s = 0;
		assertTrue(alg.isInFastBounds(rect));

		// see if it checked to see if the four corners are in bounds
		assertEquals(4,interp.list.size());
		Point2D_F32 p0 = interp.list.get(0);
		Point2D_F32 p1 = interp.list.get(1);
		Point2D_F32 p2 = interp.list.get(2);
		Point2D_F32 p3 = interp.list.get(3);

		// the order really doesn't matter, but easier to code the test this way
		assertEquals(4f-4.5f,p0.x,1e-4f);
		assertEquals(5f-9.5f,p0.y,1e-4f);

		assertEquals(4f-4.5f,p1.x,1e-4f);
		assertEquals(5f+9.5f,p1.y,1e-4f);

		assertEquals(4f+4.5f,p2.x,1e-4f);
		assertEquals(5f+9.5f,p2.y,1e-4f);

		assertEquals(4f+4.5f,p3.x,1e-4f);
		assertEquals(5f-9.5f,p3.y,1e-4f);
	}

	@Test
	public void normalizeHistogram() {
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,null);

		float total = 0;
		for( int i = 0; i < alg.histogram.length; i++ ) {
			total += alg.histogram[i] = i+1;
		}
		float expected[] = alg.histogram.clone();

		alg.normalizeHistogram();

		for( int i = 0; i < alg.histogram.length; i++ ) {
			assertEquals( expected[i]/total,alg.histogram[i],1e-4);
		}
	}

	@Test
	public void squareToImage() {
		RectangleRotate_F32 rect = new RectangleRotate_F32(4,5,10,20,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,null);

		alg.c = 1; alg.s = 0;
		alg.squareToImageSample(0,0,rect);

		assertEquals(4,alg.imageX,1e-4f);
		assertEquals(5,alg.imageY,1e-4f);

		alg.squareToImageSample(-0.5f,0.5f,rect);

		assertEquals(4f-4.5f,alg.imageX,1e-4f);
		assertEquals(5f+9.5f,alg.imageY,1e-4f);

		alg.c = 0.5f; alg.s = -0.5f;
		alg.squareToImageSample(-0.5f,0.5f,rect);
		                                            // -4.5 + 9.5
		assertEquals(4f-2.25f+4.75f,alg.imageX,1e-4f);
		assertEquals(5f+2.25f+4.75f,alg.imageY,1e-4f);
	}

	static class DummyInterpolate implements InterpolatePixelMB {

		List<Point2D_F32> list = new ArrayList<>();

		@Override
		public void setBorder(ImageBorder border) {}

		@Override
		public ImageBorder getBorder() {return null;}

		@Override
		public void setImage(ImageBase image) {}

		@Override
		public ImageBase getImage() {
			return null;
		}

		@Override
		public boolean isInFastBounds(float x, float y) {
			list.add( new Point2D_F32(x,y));
			return true;
		}

		@Override
		public int getFastBorderX() {
			return 0;
		}

		@Override
		public int getFastBorderY() {
			return 0;
		}

		@Override
		public ImageType getImageType() {
			return null;
		}

		@Override
		public void get(float x, float y, float[] values) {}

		@Override
		public void get_fast(float x, float y, float[] values) {}
	}

}
