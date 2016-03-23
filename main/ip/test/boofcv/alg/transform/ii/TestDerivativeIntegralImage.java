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

package boofcv.alg.transform.ii;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestDerivativeIntegralImage {

	Random rand = new Random(234);
	int width = 30;
	int height = 40;

	@Test
		public void kernelDerivX() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int r = 1; r < 5; r++ ) {
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivX(r,null);
			Kernel2D_F32 kernel = createDerivX(r);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void kernelDerivY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int r = 1; r < 5; r++ ) {
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivY(r,null);
			Kernel2D_F32 kernel = createDerivX(r);
			kernel = KernelMath.transpose(kernel);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void kernelHaarX() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int r = 1; r < 5; r++ ) {
			IntegralKernel kernelI = DerivativeIntegralImage.kernelHaarX(r,null);
			Kernel2D_F32 kernel = createHaarX(r);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void kernelHaarY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int i = 1; i < 5; i++ ) {
			int size = i*2;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelHaarY(size,null);
			Kernel2D_F32 kernel = createHaarX(size);
			kernel = KernelMath.transpose(kernel);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void kernelDerivXX() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivXX(size,null);
			Kernel2D_F32 kernel = createDerivXX(size);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void derivXX() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXX(size);
			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivXX(integral,found,size);

			int r = size/2;
			GrayF32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r, null);
			GrayF32 b = found.subimage(r+1,r+1,found.width-r,found.height-r, null);

			BoofTesting.assertEquals(a,b,1e-2);
		}
	}

	@Test
	public void kernelDerivYY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivYY(size,null);
			Kernel2D_F32 kernel = createDerivXX(size);
			kernel = KernelMath.transpose(kernel);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void derivYY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXX(size);
			kernel = KernelMath.transpose(kernel);

			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivYY(integral,found,size);

			int r = size/2;
			GrayF32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r, null);
			GrayF32 b = found.subimage(r+1,r+1,found.width-r,found.height-r, null);

			BoofTesting.assertEquals(a,b,1e-2);
		}
	}

	@Test
	public void kernelDerivXY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value(orig, 0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivXY(size,null);
			Kernel2D_F32 kernel = createDerivXY(size);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			BoofTesting.assertEquals(expected,found,1e-2);
		}
	}

	@Test
	public void derivXY() {
		GrayF32 orig = new GrayF32(width,height);
		GrayF32 integral = new GrayF32(width,height);

		ImageMiscOps.fillUniform(orig,rand,0,20);

		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXY(size);

			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivXY(integral,found,size);

			int r = size/2;
			GrayF32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r, null);
			GrayF32 b = found.subimage(r+1,r+1,found.width-r,found.height-r, null);

			BoofTesting.assertEquals(a,b,1e-2);
		}
	}

	private Kernel2D_F32 createDerivX( int r ) {
		int size = r*2+1;

		Kernel2D_F32 ret = new Kernel2D_F32(size);

		for( int y = 0; y < size; y++ ) {
			for( int x = 0; x < r; x++ ) {
				ret.set(x,y,-1);
				ret.set(x+r+1,y,1);
			}
		}

		return ret;
	}

	private Kernel2D_F32 createHaarX( int r ) {
		int size = r*2;

		// TODO kernels only support odd sizes right now...  change if that changes (remove +1)
		Kernel2D_F32 ret = new Kernel2D_F32(size+1);

		for( int y = 1; y <= size; y++ ) {
			for( int x = 1; x <= r; x++ ) {
				ret.set(x,y,-1);
				ret.set(x+r,y,1);
			}
		}

		return ret;
	}

	private Kernel2D_F32 createDerivXX( int size ) {
		int blockW = size/3;
		int blockH = size-blockW-1;
		int borderY = (size-blockH)/2;

		Kernel2D_F32 ret = new Kernel2D_F32(size);

		for( int y = borderY; y < size-borderY; y++ ) {
			for( int x = 0; x < blockW; x++ ) {
				ret.set(x,y,1);
				ret.set(x+blockW*2,y,1);
			}
			for( int x = blockW; x < 2*blockW; x++ ) {
				ret.set(x,y,-2);
			}
		}
		return ret;
	}

	private Kernel2D_F32 createDerivXY( int size ) {
		int block = size/3;
		int border = (size-2*block-1)/2;

		int w = block*3;

		Kernel2D_F32 ret = new Kernel2D_F32(w);

		for( int y = border; y < border+block; y++ ) {
			for( int x = border; x < block+border; x++ ) {
				ret.set(x,y,1);
				ret.set(x+block+1,y,-1);
			}
		}
		for( int y = border+block+1; y < size-border; y++ ) {
			for( int x = border; x < block+border; x++ ) {
				ret.set(x,y,-1);
				ret.set(x+block+1,y,1);
			}
		}
		return ret;
	}
}
