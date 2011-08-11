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

package gecv.alg.transform.ii;

import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.ConvolveWithBorder;
import gecv.alg.filter.kernel.KernelMath;
import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
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
	public void kernelDerivXX() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = FactoryImageBorder.value(orig,0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivXX(size);
			Kernel2D_F32 kernel = createDerivXX(size);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			GecvTesting.assertEquals(expected,found,0,1e-2);
		}
	}

	@Test
	public void derivXX() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXX(size);
			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivXX(integral,found,size);

			int r = size/2;
			ImageFloat32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r);
			ImageFloat32 b = found.subimage(r+1,r+1,found.width-r,found.height-r);

			GecvTesting.assertEquals(a,b,0,1e-2);
		}
	}

	@Test
	public void kernelDerivYY() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = FactoryImageBorder.value(orig,0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivYY(size);
			Kernel2D_F32 kernel = createDerivXX(size);
			kernel = KernelMath.transpose(kernel);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			GecvTesting.assertEquals(expected,found,0,1e-2);
		}
	}

	@Test
	public void derivYY() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXX(size);
			kernel = KernelMath.transpose(kernel);

			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivYY(integral,found,size);

			int r = size/2;
			ImageFloat32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r);
			ImageFloat32 b = found.subimage(r+1,r+1,found.width-r,found.height-r);

			GecvTesting.assertEquals(a,b,0,1e-2);
		}
	}

	@Test
	public void kernelDerivXY() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		ImageBorder_F32 border = FactoryImageBorder.value(orig,0);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			IntegralKernel kernelI = DerivativeIntegralImage.kernelDerivXY(size);
			Kernel2D_F32 kernel = createDerivXY(size);

			ConvolveWithBorder.convolve(kernel,orig,expected,border);
			IntegralImageOps.convolve(integral,kernelI,found);

			GecvTesting.assertEquals(expected,found,0,1e-2);
		}
	}

	@Test
	public void derivXY() {
		ImageFloat32 orig = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		ImageTestingOps.randomize(orig,rand,0,20);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		IntegralImageOps.transform(orig,integral);

		for( int i = 1; i <= 5; i += 2 ) {
			int size = i*3;
			Kernel2D_F32 kernel = createDerivXY(size);

			ConvolveImageNoBorder.convolve(kernel,orig,expected);
			DerivativeIntegralImage.derivXY(integral,found,size);

			int r = size/2;
			ImageFloat32 a = expected.subimage(r+1,r+1,expected.width-r,expected.height-r);
			ImageFloat32 b = found.subimage(r+1,r+1,found.width-r,found.height-r);

			GecvTesting.assertEquals(a,b,0,1e-2);
		}
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
