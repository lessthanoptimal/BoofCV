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

package boofcv.alg.tracker.circulant;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.InterleavedF64;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ejml.data.Complex64F;
import org.ejml.ops.ComplexMath64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* @author Peter Abeles
*/
public class TestCirculantTracker {

	Random rand = new Random(234);

	int width = 60;
	int height = 80;

	InterpolatePixelS<GrayF32> interp;

	public TestCirculantTracker() {
		interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.EXTENDED);
	}

	@Test
	public void meanShift() {
		int w = 32;

		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,w,255,interp);

		int peakX = 13;
		int peakY = 17;

		alg.getResponse().reshape(w,w);
		for( int i = 0; i < w; i++ ) {
			double b = Math.exp( -(i-peakY)*(i-peakY)/3.0 );
			for( int j = 0; j < w; j++ ) {
				double a = Math.exp( -(j-peakX)*(j-peakX)/3.0 );

				alg.getResponse().set(j,i,a*b);
			}
		}

		alg.subpixelPeak(peakX - 2, peakY + 1);

		assertEquals(2,alg.offX,0.3);
		assertEquals(-1,alg.offY,0.3);
	}

	@Test
	public void basicTrackingCheck() {
		GrayF32 a = new GrayF32(30,35);
		GrayF32 b = new GrayF32(30,35);

		// randomize input image and move it
		GImageMiscOps.fillUniform(a, rand, 0, 200);
		GImageMiscOps.fillUniform(b,rand,0,200);

		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,64,255,interp);
		alg.initialize(a, 5, 6, 20, 25);

		shiftCopy(2,4,a,b);
		alg.performTracking(b);

		double tolerance = 1;

		RectangleLength2D_F32 r = alg.getTargetLocation();
		assertEquals(5+2,r.x0,tolerance);
		assertEquals(6 + 4, r.y0, tolerance);
	}

	@Test
	public void computeCosineWindow() {
		GrayF64 found = new GrayF64(20,25);

		CirculantTracker.computeCosineWindow(found);

		// should be between 0 and 1
		for( int i = 0; i < found.data.length; i++ ) {
			assertTrue( found.data[i] >= 0 && found.data[i] <= 1);
		}

		centeredSymmetricChecks(found,false);
	}

	@Test
	public void computeGaussianWeights() {
		int w = 16;
		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,w,255,interp);

		alg.gaussianWeight.reshape(w,w);
		alg.gaussianWeightDFT.reshape(w, w);

		alg.computeGaussianWeights(w);

		centeredSymmetricChecks(alg.gaussianWeight,true);
	}

	private void centeredSymmetricChecks(GrayF64 image , boolean offByOne ) {

		// see comments in computeGaussianWeights
		int offX = offByOne ? 1-image.width%2 : 0;
		int offY = offByOne ? 1-image.height%2 : 0;

		int cx = image.width/2;
		int cy = image.height/2;
		int w = image.width-1;
		int h = image.height-1;

		// edges should be smaller than center
		assertTrue(image.get(cx, cy) > image.get(0, 0));
		assertTrue( image.get(cx,cy) > image.get(w,h) );
		assertTrue( image.get(cx,cy) > image.get(w,h) );
		assertTrue(image.get(cx, cy) > image.get(w, 0));

		// symmetry check
		for( int i = offY; i < cy; i++ ) {
			for( int j = offX; j < cx; j++ ) {
				double v0 = image.get(j,i);
				double v1 = image.get(w-j+offX,i);
				double v2 = image.get(j,h-i+offY);
				double v3 = image.get(w-j+offX,h-i+offY);

				assertEquals(v0,v1,1e-4);
				assertEquals(v0,v2,1e-4);
				assertEquals(i+" "+j,v0,v3,1e-4);
			}
		}
	}

	/**
	 * Check a few simple motions.  It seems to be accurate to within 1 pixel.  Considering alphas seems to be the issue
	 */
	@Test
	public void updateTrackLocation() {
		GrayF32 a = new GrayF32(100,100);
		GrayF32 b = new GrayF32(100,100);

		// randomize input image and move it
		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(0,0,a,b);

		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,64,255,interp);
		alg.initialize(a,5,6,20,25);

		alg.updateTrackLocation(b);

		// only pixel level precision.
		float tolerance = 1f;

		// No motion motion
		RectangleLength2D_F32 r = alg.getTargetLocation();
		assertEquals(5,r.x0,tolerance);
		assertEquals(6,r.y0,tolerance);

		// check estimated motion
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(-3,2,a,b);
		alg.updateTrackLocation(b);
		r = alg.getTargetLocation();
		assertEquals(5-3,r.x0,tolerance);
		assertEquals(6+2,r.y0,tolerance);

		// try out of bounds case
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(-6,0,a,b);
		alg.updateTrackLocation(b);
		assertEquals(5-6,r.x0,tolerance);
		assertEquals(6,r.y0,tolerance);
	}

	@Test
	public void performLearning() {
		float interp_factor = 0.075f;

		GrayF32 a = new GrayF32(20,25);
		GrayF32 b = new GrayF32(20,25);

		ImageMiscOps.fill(a, 100);
		ImageMiscOps.fill(b,200);

		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,64,255,interp);
		alg.initialize(a,0,0,20,25);

		// copy its internal value
		GrayF64 templateC = new GrayF64(alg.template.width,alg.template.height);
		templateC.setTo(alg.template);

		// give it two images
		alg.performLearning(b);

		// make sure the images aren't full of zero
		assertTrue(Math.abs(ImageStatistics.sum(templateC)) > 0.1 );
		assertTrue(Math.abs(ImageStatistics.sum(alg.template)) > 0.1 );

		int numNotSame = 0;
		// the result should be an average of the two
		for( int i = 0; i < a.data.length; i++ ) {
			if( Math.abs(a.data[i]-alg.templateNew.data[i]) > 1e-4 )
				numNotSame++;

			// should be more like the original one than the new one
			double expected = templateC.data[i]*(1-interp_factor) + interp_factor*alg.templateNew.data[i];
			double found = alg.template.data[i];

			assertEquals(expected,found,1e-4);
		}

		// make sure it is actually different
		assertTrue(numNotSame>100);
	}

	@Test
	public void dense_gauss_kernel() {
		// try several different shifts
		dense_gauss_kernel(0,0);
		dense_gauss_kernel(5,0);
		dense_gauss_kernel(0,5);
		dense_gauss_kernel(-3,-2);
	}

	public void dense_gauss_kernel( int offX , int offY ) {
		GrayF64 region = new GrayF64(32,32);
		GrayF64 target = new GrayF64(32,32);
		GrayF64 k = new GrayF64(32,32);

		CirculantTracker<GrayF32> alg = new CirculantTracker<>(1f/16,0.2,1e-2,0.075,1.0,32,255,interp);
		alg.initialize(new GrayF32(32,32),0,0,32,32);

		// create a shape inside the image
		GImageMiscOps.fillRectangle(region,200,10,15,5,7);

		// copy a shifted portion of the region
		shiftCopy(offX, offY, region, target);

		// process and see if the peak is where it should be
		alg.dense_gauss_kernel(0.2f,region,target,k);

		int maxX=-1,maxY=-1;
		double maxValue = -1;
		for( int y = 0; y < k.height;y++ ){
			for( int x=0; x < k.width;x++ ) {
				if( k.get(x,y) > maxValue ) {
					maxValue = k.get(x,y);
					maxX = x;
					maxY = y;
				}
			}
		}

		int expectedX = k.width/2-offX;
		int expectedY = k.height/2-offY;

		assertEquals(expectedX,maxX);
		assertEquals(expectedY,maxY);
	}

	private void shiftCopy(int offX, int offY, GrayF32 src, GrayF32 dst) {
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int xx = x + offX;
				int yy = y + offY;

				if( xx >= 0 && xx < src.width && yy >= 0 && yy < src.height ) {
					dst.set(xx, yy, src.get(x, y));
				}
			}
		}
	}

	private void shiftCopy(int offX, int offY, GrayF64 src, GrayF64 dst) {
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int xx = x + offX;
				int yy = y + offY;

				if( xx >= 0 && xx < src.width && yy >= 0 && yy < src.height ) {
					dst.set(xx, yy, src.get(x, y));
				}
			}
		}
	}

	@Test
	public void imageDotProduct() {
		GrayF64 a = new GrayF64(width,height);
		ImageMiscOps.fillUniform(a,rand,0,10);

		double total = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				total += a.get(x,y)*a.get(x,y);
			}
		}
		double found = CirculantTracker.imageDotProduct(a);
		assertEquals(total,found,1e-8);
	}

	@Test
	public void elementMultConjB() {
		InterleavedF64 a = new InterleavedF64(width,height,2);
		InterleavedF64 b = new InterleavedF64(width,height,2);
		InterleavedF64 c = new InterleavedF64(width,height,2);

		ImageMiscOps.fillUniform(a,rand,-10,10);
		ImageMiscOps.fillUniform(b,rand,-10,10);
		ImageMiscOps.fillUniform(c,rand,-10,10);

		CirculantTracker.elementMultConjB(a, b, c);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Complex64F aa = new Complex64F(a.getBand(x,y,0),a.getBand(x,y,1));
				Complex64F bb = new Complex64F(b.getBand(x,y,0),b.getBand(x,y,1));

				Complex64F cc = new Complex64F();
				ComplexMath64F.conj(bb, bb);
				ComplexMath64F.multiply(aa, bb, cc);

				double foundReal = c.getBand(x,y,0);
				double foundImg = c.getBand(x,y,1);

				assertEquals(cc.real,foundReal,1e-4);
				assertEquals(cc.imaginary,foundImg,1e-4);
			}
		}
	}

	@Test
	public void computeAlphas() {
		InterleavedF64 yf = new InterleavedF64(width,height,2);
		InterleavedF64 kf = new InterleavedF64(width,height,2);
		InterleavedF64 alphaf = new InterleavedF64(width,height,2);

		ImageMiscOps.fillUniform(yf,rand,-10,10);
		ImageMiscOps.fillUniform(kf,rand,-10,10);
		ImageMiscOps.fillUniform(alphaf,rand,-10,10);

		float lambda = 0.01f;
		CirculantTracker.computeAlphas(yf, kf, lambda, alphaf);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Complex64F a = new Complex64F(yf.getBand(x,y,0),yf.getBand(x,y,1));
				Complex64F b = new Complex64F(kf.getBand(x,y,0)+lambda,kf.getBand(x,y,1));

				Complex64F c = new Complex64F();
				ComplexMath64F.divide(a, b, c);

				double foundReal = alphaf.getBand(x,y,0);
				double foundImg = alphaf.getBand(x,y,1);

				assertEquals(c.real,foundReal,1e-4);
				assertEquals(c.imaginary,foundImg,1e-4);
			}
		}
	}
}