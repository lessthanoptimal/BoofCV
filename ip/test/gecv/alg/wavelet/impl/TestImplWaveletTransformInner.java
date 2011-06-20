/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.wavelet.impl;

import gecv.alg.misc.ImageTestingOps;
import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletDesc_F32;
import gecv.core.image.border.BorderIndex1D_Wrap;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
// todo test sub-image
public class TestImplWaveletTransformInner {

	Random rand = new Random(234);

	int width = 20;
	int height = 30;


	@Test
	public void horizontal() {
		// test even and odd width images
		for( int shrink = 0; shrink <= 1; shrink++ ) {
			ImageFloat32 input = new ImageFloat32(width-shrink,height);
			ImageFloat32 found = new ImageFloat32(width,height);
			ImageFloat32 expected = new ImageFloat32(width,height);

			ImageTestingOps.randomize(input,rand,0,50);

			// test different descriptions lengths and offsets
			for( int o = 0; o <= 2; o++ ) {
				for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
					ImageTestingOps.fill(found,0);
					ImageTestingOps.fill(expected,0);
					WaveletDesc_F32 desc = createDesc(-o,l);

					ImplWaveletTransformNaive.horizontal(desc,input,expected);
					ImplWaveletTransformInner.horizontal(desc,input,found);

					this.equalsTranHorizontal(desc,expected,found,shrink != 0);
				}
			}
		}
	}

	@Test
	public void vertical() {
		// test even and odd width images
		for( int shrink = 1; shrink <= 1; shrink++ ) {
			ImageFloat32 input = new ImageFloat32(width,height-shrink);
			ImageFloat32 found = new ImageFloat32(width,height);
			ImageFloat32 expected = new ImageFloat32(width,height);

			ImageTestingOps.randomize(input,rand,0,50);

			// test different descriptions lengths and offsets
			for( int o = 0; o <= 2; o++ ) {
				for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
					ImageTestingOps.fill(found,0);
					ImageTestingOps.fill(expected,0);
					WaveletDesc_F32 desc = createDesc(-o,l);

					ImplWaveletTransformNaive.vertical(desc,input,expected);
					ImplWaveletTransformInner.vertical(desc,input,found);

					equalsTranVertical(desc,expected,found,shrink != 0);
				}
			}
		}
	}

	@Test
	public void horizontalInverse() {
		// test even and odd width images
		for( int shrink = 0; shrink <= 1; shrink++ ) {
			ImageFloat32 transform = new ImageFloat32(width,height);
			ImageFloat32 found = new ImageFloat32(width-shrink,height);
			ImageFloat32 expected = new ImageFloat32(width-shrink,height);

			ImageTestingOps.randomize(transform,rand,0,50);

			// test different descriptions lengths and offsets
			for( int o = 0; o <= 2; o++ ) {
				for( int l = Math.min(3,2+o); l <= 5; l++ ) {
					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
					ImageTestingOps.fill(found,2);
					ImageTestingOps.fill(expected,2);
					WaveletDesc_F32 desc = createDesc(-o,l);

					ImplWaveletTransformNaive.horizontalInverse(desc,transform,expected);
					ImplWaveletTransformInner.horizontalInverse(desc,transform,found);

					int border = Math.max(UtilWavelet.computeBorderStart(desc),
							UtilWavelet.computeBorderEnd(desc,width-shrink))+o*2;

					GecvTesting.assertEquals(expected,found,border,1e-4f);
				}
			}
		}
	}

	@Test
	public void verticalInverse() {
		// test even and odd width images
		for( int shrink = 0; shrink <= 1; shrink++ ) {
			ImageFloat32 transform = new ImageFloat32(width,height);
			ImageFloat32 found = new ImageFloat32(width-shrink,height);
			ImageFloat32 expected = new ImageFloat32(width-shrink,height);

			ImageTestingOps.randomize(transform,rand,0,50);

			// test different descriptions lengths and offsets
			for( int o = 0; o <= 2; o++ ) {
				for( int l = Math.min(3,2+o); l <= 5; l++ ) {
					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
					ImageTestingOps.fill(found,2);
					ImageTestingOps.fill(expected,2);
					WaveletDesc_F32 desc = createDesc(-o,l);

					ImplWaveletTransformNaive.verticalInverse(desc,transform,expected);
					ImplWaveletTransformInner.verticalInverse(desc,transform,found);

					int border = Math.max(UtilWavelet.computeBorderStart(desc),
							UtilWavelet.computeBorderEnd(desc,height-shrink))+o*2;

					GecvTesting.assertEquals(expected,found,border,1e-4f);
				}
			}
		}
	}

	private void equalsTranHorizontal( WaveletDesc_F32 desc,
								   ImageFloat32 expected , ImageFloat32 found , boolean isOdd ) {
		int minus = isOdd ? -1 : 0;
		int begin = UtilWavelet.computeBorderStart(desc);
		int end = expected.getWidth()-UtilWavelet.computeBorderEnd(desc,expected.width+minus);

		int w = expected.width;
		int h = expected.height;

		equalsTranHorizontal(expected.subimage(0,0,w/2,h),found.subimage(0,0,w/2,h),begin/2,end/2,"left");
		equalsTranHorizontal(expected.subimage(w/2,h,w,h),found.subimage(w/2,h,w,h),begin/2,end/2,"right");
	}

	private void equalsTranHorizontal( ImageFloat32 expected , ImageFloat32 found ,
								   int begin , int end , String quad ) {

		for( int y = 0; y < expected.height; y++ ) {
			for( int x = 0; x < expected.width; x++ ) {
				// see if the inner image is identical to the naive implementation
				// the border should be unmodified, zeros
				if( x >= begin && x < end )
					assertEquals(quad+" ( "+x+" , "+y+" )",expected.get(x,y) , found.get(x,y) , 1e-4f);
				else
					assertTrue(quad+" ( "+x+" , "+y+" ) "+found.get(x,y),0 == found.get(x,y));
			}
		}
	}

	private void equalsTranVertical( WaveletDesc_F32 desc,
								   ImageFloat32 expected , ImageFloat32 found , boolean isOdd ) {
		int minus = isOdd ? -1 : 0;
		int begin = UtilWavelet.computeBorderStart(desc);
		int end = expected.getHeight()-UtilWavelet.computeBorderEnd(desc,expected.height+minus);

		int w = expected.width;
		int h = expected.height;

		equalsTranVertical(expected.subimage(0,0,w,h/2),found.subimage(0,0,w,h/2),begin/2,end/2,"top");
		equalsTranVertical(expected.subimage(w,h/2,w,h),found.subimage(w,h/2,w,h),begin/2,end/2,"bottom");
	}

	private void equalsTranVertical( ImageFloat32 expected , ImageFloat32 found ,
								   int begin , int end , String quad ) {

		for( int y = 0; y < expected.height; y++ ) {
			// see if the inner image is identical to the naive implementation
			// the border should be unmodified, zeros
			if( y >= begin && y < end ) {
				for( int x = 0; x < expected.width; x++ ) {
					assertEquals(quad+" ( "+x+" , "+y+" )",expected.get(x,y) , found.get(x,y) , 1e-4f);
				}
			} else {
				for( int x = 0; x < expected.width; x++ ) {
					assertTrue(quad+" ( "+x+" , "+y+" ) "+found.get(x,y),0 == found.get(x,y));
				}
			}
		}
	}


	private WaveletDesc_F32 createDesc(int offset, int length) {
		WaveletDesc_F32 ret = new WaveletDesc_F32();
		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = offset;
		ret.offsetWavelet = offset;
		ret.scaling = new float[length];
		ret.wavelet = new float[length];

		for( int i = 0; i < length; i++ ) {
			ret.scaling[i] = (float)rand.nextGaussian();
			ret.wavelet[i] = (float)rand.nextGaussian();
		}

		return ret;
	}
}
