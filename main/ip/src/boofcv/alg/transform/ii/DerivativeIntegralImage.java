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

import boofcv.struct.image.GrayF32;


/**
 * @author Peter Abeles
 */
public class DerivativeIntegralImage {

	/**
	 * Creates a kernel for a symmetric box derivative.
	 *
	 * @param r Radius of the box.  width is 2*r+1
	 * @return Kernel Kernel for derivative.
	 */
	public static IntegralKernel kernelDerivX( int r , IntegralKernel ret ) {
		if( ret == null )
			ret = new IntegralKernel(2);

		ret.blocks[0].set(-r-1,-r-1,-1,r);
		ret.blocks[1].set(0,-r-1,r,r);
		ret.scales[0] = -1;
		ret.scales[1] = 1;

		return ret;
	}

	/**
	 * Creates a kernel for a symmetric box derivative.
	 *
	 * @param r Radius of the box.  width is 2*r+1
	 * @return Kernel Kernel for derivative.
	 */
	public static IntegralKernel kernelDerivY( int r , IntegralKernel ret ) {
		if( ret == null )
			ret = new IntegralKernel(2);

		ret.blocks[0].set(-r-1,-r-1,r,-1);
		ret.blocks[1].set(-r-1,0,r,r);
		ret.scales[0] = -1;
		ret.scales[1] = 1;

		return ret;
	}


	/**
	 * Creates a kernel for the Haar wavelet "centered" around the target pixel.
	 *
	 * @param r Radius of the box.  width is 2*r
	 * @return Kernel for a Haar x-axis wavelet.
	 */
	public static IntegralKernel kernelHaarX( int r , IntegralKernel ret) {
		if( ret == null )
			ret = new IntegralKernel(2);

		ret.blocks[0].set(-r, -r, 0, r);
		ret.blocks[1].set(0,-r,r,r);
		ret.scales[0] = -1;
		ret.scales[1] = 1;

		return ret;
	}

	/**
	 * Creates a kernel for the Haar wavelet "centered" around the target pixel.
	 *
	 * @param r Radius of the box.  width is 2*r
	 * @return Kernel for a Haar y-axis wavelet.
	 */
	public static IntegralKernel kernelHaarY( int r , IntegralKernel ret) {
		if( ret == null )
			ret = new IntegralKernel(2);

		ret.blocks[0].set(-r,-r,r,0);
		ret.blocks[1].set(-r,0,r,r);
		ret.scales[0] = -1;
		ret.scales[1] = 1;

		return ret;
	}

	public static IntegralKernel kernelDerivXX( int size , IntegralKernel ret ) {
		if( ret == null )
			ret = new IntegralKernel(2);

		// lobe size
		int blockW = size/3;
		// horizontal band size
		int blockH = size-blockW-1;

		int r1 = blockW/2;
		int r2 = blockW+r1;
		int r3 = blockH/2;

		ret.blocks[0].set(-r2-1,-r3-1,r2,r3);
		ret.blocks[1].set(-r1 - 1, -r3 - 1, r1, r3);
		ret.scales[0] = 1;
		ret.scales[1] = -3;

		return ret;
	}

	public static IntegralKernel kernelDerivYY( int size , IntegralKernel ret ) {
		if( ret == null )
			ret = new IntegralKernel(2);

		int blockW = size/3;
		int blockH = size-blockW-1;

		int r1 = blockW/2;
		int r2 = blockW+r1;
		int r3 = blockH/2;

		ret.blocks[0].set(-r3-1,-r2-1,r3,r2);
		ret.blocks[1].set(-r3-1,-r1-1,r3,r1);
		ret.scales[0] = 1;
		ret.scales[1] = -3;

		return ret;
	}

	public static IntegralKernel kernelDerivXY( int size , IntegralKernel ret ) {
		if( ret == null )
			ret = new IntegralKernel(4);

		int block = size/3;

		ret.blocks[0].set(-block-1,-block-1,-1,-1);
		ret.blocks[1].set(0,-block-1,block,-1);
		ret.blocks[2].set(0, 0, block, block);
		ret.blocks[3].set(-block-1,0,-1,block);
		ret.scales[0] = 1;
		ret.scales[1] = -1;
		ret.scales[2] = 1;
		ret.scales[3] = -1;

		return ret;
	}

	public static void derivXX(GrayF32 input , GrayF32 output , int size )
	{
		int blockW = size/3;
		int blockH = size-blockW-1;
		int radiusW = size/2;
		int radiusH = blockH/2;

		int blockW2 = 2*blockW;
		int blockW3 = 3*blockW;

		int endY = input.height - radiusH;
		int endX = input.width - radiusW;

		for( int y = radiusH+1; y < endY; y++ ) {
			int indexTop = input.startIndex + (y-radiusH-1)*input.stride;
			int indexBottom = indexTop + (blockH)*input.stride;
			int indexDst = output.startIndex + y*output.stride+radiusW+1;

			for( int x = radiusW+1; x < endX; x++ , indexTop++,indexBottom++,indexDst++) {
				float sum = input.data[indexBottom+blockW3] - input.data[indexTop+blockW3] - input.data[indexBottom] + input.data[indexTop];
				sum -= 3*(input.data[indexBottom+blockW2] - input.data[indexTop+blockW2] - input.data[indexBottom+blockW] + input.data[indexTop+blockW]);

				output.data[indexDst] = sum;
			}
		}
	}

	public static void derivYY(GrayF32 input , GrayF32 output , int size )
	{
		int blockH = size/3;
		int blockW = size-blockH-1;
		int radiusH = size/2;
		int radiusW = blockW/2;

		int rowOff1 = blockH*input.stride;
		int rowOff2 = 2*rowOff1;
		int rowOff3 = 3*rowOff1;

		int endY = input.height - radiusH;
		int endX = input.width - radiusW;

		for( int y = radiusH+1; y < endY; y++ ) {
			int indexL = input.startIndex + (y-radiusH-1)*input.stride;
			int indexR = indexL + blockW;
			int indexDst = output.startIndex + y*output.stride+radiusW+1;

			for( int x = radiusW+1; x < endX; x++ , indexL++,indexR++,indexDst++) {
				float sum = input.data[indexR+rowOff3] - input.data[indexL+rowOff3] - input.data[indexR] + input.data[indexL];
				sum -= 3*(input.data[indexR+rowOff2] - input.data[indexL+rowOff2] - input.data[indexR+rowOff1] + input.data[indexL+rowOff1]);

				output.data[indexDst] = sum;
			}
		}
	}

	public static void derivXY(GrayF32 input , GrayF32 output , int size )
	{
		int block = size/3;

		int endY = input.height - block;
		int endX = input.width - block;

		for( int y = block+1; y < endY; y++ ) {
			int indexY1 = input.startIndex + (y-block-1)*input.stride;
			int indexY2 = indexY1 + block*input.stride;
			int indexY3 = indexY2 + input.stride;
			int indexY4 = indexY3 + block*input.stride;
			int indexDst = output.startIndex + y*output.stride+block+1;

			for( int x = block+1; x < endX; x++ , indexY1++,indexY2++,indexY3++,indexY4++,indexDst++) {
				int x3 = block+1;
				int x4 = x3+block;

				float sum = input.data[indexY2+block] - input.data[indexY1+block] - input.data[indexY2] + input.data[indexY1];
				sum -= input.data[indexY2+x4] - input.data[indexY1+x4] - input.data[indexY2+x3] + input.data[indexY1+x3];
				sum += input.data[indexY4+x4] - input.data[indexY3+x4] - input.data[indexY4+x3] + input.data[indexY3+x3];
				sum -= input.data[indexY4+block] - input.data[indexY3+block] - input.data[indexY4] + input.data[indexY3];

				output.data[indexDst] = sum;
			}
		}
	}
}
