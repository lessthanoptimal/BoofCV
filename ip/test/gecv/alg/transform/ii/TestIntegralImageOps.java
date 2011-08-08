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

import gecv.alg.filter.convolve.ConvolveWithBorder;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.ImageRectangle;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestIntegralImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;


	@Test
	public void convolve() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,10);
		IntegralImageOps.transform(input,integral);

		Kernel2D_F32 kernel = new Kernel2D_F32(new float[]{1,1,1,2,2,2,1,1,1},3);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		ImageBorder_F32 border = FactoryImageBorder.value(input,0);
		ConvolveWithBorder.convolve(kernel,input,expected,border);

		ImageRectangle blocks[] = new ImageRectangle[2];
		blocks[0] = new ImageRectangle(-2,-2,1,1);
		blocks[1] = new ImageRectangle(-2,-1,1,0);

		IntegralImageOps.convolve(integral,blocks,new int[]{1,1},found);

		GecvTesting.assertEquals(expected,found,0,1e-4f);
	}

	@Test
	public void convolveBorder() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,10);
		IntegralImageOps.transform(input,integral);

		Kernel2D_F32 kernel = new Kernel2D_F32(new float[]{1,1,1,2,2,2,1,1,1},3);

		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		ImageBorder_F32 border = FactoryImageBorder.value(input,0);
		ConvolveWithBorder.convolve(kernel,input,expected,border);

		ImageRectangle blocks[] = new ImageRectangle[2];
		blocks[0] = new ImageRectangle(-2,-2,1,1);
		blocks[1] = new ImageRectangle(-2,-1,1,0);

		IntegralImageOps.convolveBorder(integral,blocks,new int[]{1,1},found,4,5);

		GecvTesting.assertEqualsBorder(expected,found,1e-4f,4,5);
	}

	@Test
	public void convolveSparse() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,10);
		IntegralImageOps.transform(input,integral);


		ImageFloat32 expected = new ImageFloat32(width,height);

		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernel.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernel.scales =  new int[]{1,2};

		IntegralImageOps.convolve(integral,kernel.blocks,kernel.scales,expected);

		assertEquals(expected.get(0,0),IntegralImageOps.convolveSparse(integral,kernel,0,0),1e-4f);
		assertEquals(expected.get(10,12),IntegralImageOps.convolveSparse(integral,kernel,10,12),1e-4f);
		assertEquals(expected.get(19,29),IntegralImageOps.convolveSparse(integral,kernel,19,29),1e-4f);
	}

	@Test
	public void block_unsafe() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		GeneralizedImageOps.fill(input,1);
		IntegralImageOps.transform(input,integral);

		assertEquals(12,IntegralImageOps.block_unsafe(integral,4,5,8,8),1e-4f);
	}

	@Test
	public void block_zero() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 integral = new ImageFloat32(width,height);

		GeneralizedImageOps.fill(input,1);
		IntegralImageOps.transform(input,integral);

		assertEquals(12,IntegralImageOps.block_zero(integral,4,5,8,8),1e-4f);

		assertEquals(12,IntegralImageOps.block_zero(integral,-1,-2,2,3),1e-4f);

		assertEquals(2,IntegralImageOps.block_zero(integral,width-2,height-3,width+1,height+3),1e-4f);

		assertEquals(0,IntegralImageOps.block_zero(integral,-3,-4,-1,-1),1e-4f);
		assertEquals(0,IntegralImageOps.block_zero(integral,width+1,height+2,width+6,height+8),1e-4f);
	}
}
