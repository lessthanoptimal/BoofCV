/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.normalized;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.InterleavedU16;
import boofcv.struct.image.InterleavedU8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalizedNaive_IL {

	Random rand = new Random(234);
	int width = 15;
	int height = 16;
	int numBands = 2;

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test
	public void horizontal() {
		Kernel1D_S32 kernel = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);

		InterleavedU8 input = new InterleavedU8(width,height, numBands);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		InterleavedU8 output = new InterleavedU8(width,height, numBands);

		ConvolveNormalizedNaive_IL.horizontal(kernel,input,output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				for (int band = 0; band < numBands; band++) {
					int expected = horizontal(x,y,band,kernel,input);
					int found = output.getBand(x,y,band);
					assertEquals(x+"  "+y,expected,found);
				}
			}
		}
	}

	private int horizontal(int x , int y , int band , Kernel1D_S32 kernel , InterleavedU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			if (image.isInBounds(x + i - kernel.offset, y)) {
				int w = kernel.get(i);
				int v = image.getBand(x + i - kernel.offset, y, band);

				total += w * v;
				weight += w;
			}
		}

		return (total + weight/2)/weight;
	}

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test
	public void vertical() {
		Kernel1D_S32 kernel = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);

		InterleavedU8 input = new InterleavedU8(width,height, numBands);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		InterleavedU8 output = new InterleavedU8(width,height, numBands);

		ConvolveNormalizedNaive_IL.vertical(kernel, input, output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				for (int band = 0; band < numBands; band++) {
					int expected = vertical(x, y,band, kernel, input);
					int found = output.getBand(x,y, band);
					assertEquals(x+"  "+y,expected,found);
				}
			}
		}
	}

	private int vertical(int x , int y , int band, Kernel1D_S32 kernel , InterleavedU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			if( image.isInBounds(x,y+i-kernel.offset)) {
				int w = kernel.get(i);
				int v = image.getBand(x,y+i-kernel.offset, band);

				total += w*v;
				weight += w;
			}
		}

		return (total + weight/2)/weight;
	}

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test
	public void vertical2_U16_U8() {
		Kernel1D_S32 kernelY = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);
		Kernel1D_S32 kernelX = new Kernel1D_S32(new int[]{4,2,1,4,3,6}, 5, 2);

		InterleavedU16 input = new InterleavedU16(width,height, numBands);
		ImageMiscOps.fillUniform(input, rand, 0, 80);
		InterleavedU8 output = new InterleavedU8(width,height, numBands);

		ConvolveNormalizedNaive_IL.vertical(kernelX,kernelY, input, output);

		InterleavedU8 alt = new InterleavedU8(width,height, numBands);
		ConvolveImageNoBorder.vertical(kernelY, input, alt, kernelX.computeSum() * kernelY.computeSum());

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				for (int band = 0; band < numBands; band++) {
					int expected = vertical2(x, y, band, kernelX, kernelY, input);
					int found = output.getBand(x,y, band);
					assertEquals(x+"  "+y,expected,found);
				}
			}
		}
	}

	private int vertical2(int x, int y, int band,
						  Kernel1D_S32 kernelX, Kernel1D_S32 kernelY,
						  InterleavedU16 image)
	{
		int total = 0;
		int weightY = 0;

		for (int i = 0; i < kernelY.width; i++) {
			if( image.isInBounds(x,y+i-kernelY.offset)) {
				int w = kernelY.get(i);
				int v = image.getBand(x,y+i-kernelY.offset,band);

				total += w*v;
				weightY += w;
			}
		}

		int weightX = 0;
		for (int i = 0; i < kernelX.width; i++) {
			if( image.isInBounds(x+i-kernelX.offset,y)) {
				int w = kernelX.get(i);

				weightX += w;
			}
		}

		int weight = weightX*weightY;
		return (total + weight/2)/weight;
	}

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test
	public void convolve() {
		Kernel2D_S32 kernel = FactoryKernel.random2D_I32(7,3,0,20,rand);
		kernel.offset = 1;

		InterleavedU8 input = new InterleavedU8(width,height, numBands);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		InterleavedU8 output = new InterleavedU8(width,height, numBands);

		ConvolveNormalizedNaive_IL.convolve(kernel, input, output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				for (int band = 0; band < numBands; band++) {
					int expected = convolve(x, y, band, kernel, input);
					int found = output.getBand(x,y,band);
					assertEquals(x+"  "+y,expected,found);
				}
			}
		}
	}

	private int convolve(int cx , int cy , int band, Kernel2D_S32 kernel , InterleavedU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			int y = cy+i-kernel.offset;
			for (int j = 0; j < kernel.width; j++) {
				int x = cx+j-kernel.offset;

				if( image.isInBounds(x,y)) {
					int w = kernel.get(j,i);
					int v = image.getBand(x,y,band);
					weight += w;
					total += w*v;
				}
			}
		}

		return (total + weight/2)/weight;
	}
}
