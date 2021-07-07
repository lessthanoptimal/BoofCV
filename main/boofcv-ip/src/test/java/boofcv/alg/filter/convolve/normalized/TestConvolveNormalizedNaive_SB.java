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

package boofcv.alg.filter.convolve.normalized;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalizedNaive_SB extends BoofStandardJUnit {

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test void horizontal() {
		Kernel1D_S32 kernel = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);

		GrayU8 input = new GrayU8(15,16);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		GrayU8 output = new GrayU8(15,16);

		ConvolveNormalizedNaive_SB.horizontal(kernel,input,output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int expected = horizontal(x,y,kernel,input);
				int found = output.get(x,y);
				assertEquals(expected,found);
			}
		}
	}

	private int horizontal(int x , int y , Kernel1D_S32 kernel , GrayU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			if( image.isInBounds(x+i-kernel.offset,y)) {
				int w = kernel.get(i);
				int v = image.get(x+i-kernel.offset,y);

				total += w*v;
				weight += w;
			}
		}

		return (total + weight/2)/weight;
	}

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test void vertical() {
		Kernel1D_S32 kernel = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);

		GrayU8 input = new GrayU8(15,16);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		GrayU8 output = new GrayU8(15,16);

		ConvolveNormalizedNaive_SB.vertical(kernel, input, output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int expected = vertical(x, y, kernel, input);
				int found = output.get(x,y);
				assertEquals(expected,found);
			}
		}
	}

	private int vertical(int x , int y , Kernel1D_S32 kernel , GrayU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			if( image.isInBounds(x,y+i-kernel.offset)) {
				int w = kernel.get(i);
				int v = image.get(x,y+i-kernel.offset);

				total += w*v;
				weight += w;
			}
		}

		return (total + weight/2)/weight;
	}

	/**
	 * Check it against one specific type to see if the core algorithm is correct
	 */
	@Test void vertical2_U16_U8() {
		Kernel1D_S32 kernelY = new Kernel1D_S32(new int[]{1,2,3,4,5,6}, 6, 4);
		Kernel1D_S32 kernelX = new Kernel1D_S32(new int[]{4,2,1,4,3,6}, 5, 2);

		GrayU16 input = new GrayU16(15,16);
		ImageMiscOps.fillUniform(input, rand, 0, 80);
		GrayU8 output = new GrayU8(15,16);

		ConvolveNormalizedNaive_SB.vertical(kernelX,kernelY, input, output);

		GrayU8 alt = new GrayU8(15,16);
		ConvolveImageNoBorder.vertical(kernelY, input, alt, kernelX.computeSum() * kernelY.computeSum(), null);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int expected = vertical2(x, y, kernelX, kernelY, input);
				int found = output.get(x,y);
				assertEquals(expected,found);
			}
		}
	}

	private int vertical2(int x, int y,
						  Kernel1D_S32 kernelX, Kernel1D_S32 kernelY,
						  GrayU16 image)
	{
		int total = 0;
		int weightY = 0;

		for (int i = 0; i < kernelY.width; i++) {
			if( image.isInBounds(x,y+i-kernelY.offset)) {
				int w = kernelY.get(i);
				int v = image.get(x,y+i-kernelY.offset);

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
	@Test void convolve() {
		Kernel2D_S32 kernel = FactoryKernel.random2D_I32(7,3,0,20,rand);
		kernel.offset = 1;

		GrayU8 input = new GrayU8(15,16);
		ImageMiscOps.fillUniform(input, rand, 0, 50);
		GrayU8 output = new GrayU8(15,16);

		ConvolveNormalizedNaive_SB.convolve(kernel, input, output);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int expected = convolve(x, y, kernel, input);
				int found = output.get(x,y);
				assertEquals(expected,found);
			}
		}
	}

	private int convolve(int cx , int cy , Kernel2D_S32 kernel , GrayU8 image )
	{
		int total = 0;
		int weight = 0;

		for (int i = 0; i < kernel.width; i++) {
			int y = cy+i-kernel.offset;
			for (int j = 0; j < kernel.width; j++) {
				int x = cx+j-kernel.offset;

				if( image.isInBounds(x,y)) {
					int w = kernel.get(j,i);
					int v = image.get(x,y);
					weight += w;
					total += w*v;
				}
			}
		}

		return (total + weight/2)/weight;
	}
}
