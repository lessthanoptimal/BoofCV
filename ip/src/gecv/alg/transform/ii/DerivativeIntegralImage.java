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

import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class DerivativeIntegralImage {

	public static void derivXX( ImageFloat32 input , ImageFloat32 output , int level )
	{
		int blockW = 3+2*level;
		int blockH = 5+4*level;
		int radiusW = 3*blockW/2;
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

	public static void derivYY( ImageFloat32 input , ImageFloat32 output , int level )
	{
		int blockH = 3+2*level;
		int blockW = 5+4*level;
		int radiusH = 3*blockH/2;
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

	public static void derivXY( ImageFloat32 input , ImageFloat32 output , int level )
	{
		int block = 3+2*level;

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
