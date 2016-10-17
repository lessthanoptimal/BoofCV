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

package boofcv.deepboof;

import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.tensors.Tensor_F32;

/**
 * Functions for manipulating data by transforming it or converting its format.  For use with DeepBoof
 *
 * @author Peter Abeles
 */
public class DataManipulationOps {

	/**
	 * Normalizes a gray scale image by first subtracting the mean then dividing by stdev.
	 * @param image Image that is to be normalized
	 * @param mean Value which is subtracted by it
	 * @param stdev The divisor
	 */
	public static void normalize(GrayF32 image , float mean , float stdev ) {
		for (int y = 0; y < image.height; y++) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;
			while( index < end ) {
				image.data[index] = (image.data[index]-mean)/stdev;
				index++;
			}
		}
	}

	/**
	 * Converts the double array into a 1D float kernel
	 * @param kernel Kernel in array format
	 * @return The kernel
	 */
	public static Kernel1D_F32 create1D_F32( double[] kernel ) {
		Kernel1D_F32 k = new Kernel1D_F32(kernel.length,kernel.length/2);
		for (int i = 0; i < kernel.length; i++) {
			k.data[i] = (float)kernel[i];
		}
		return k;
	}

	/**
	 * Converts an image into a spatial tensor
	 *
	 * @param input BoofCV planar image
	 * @param output Tensor
	 * @param miniBatch Which mini-batch in the tensor should the image be written to
	 */
	public static void imageToTensor(Planar<GrayF32> input , Tensor_F32 output , int miniBatch) {
		if( input.isSubimage())
			throw new RuntimeException("Subimages not accepted");
		if( output.getDimension() != 4 )
			throw new IllegalArgumentException("Output should be 4-DOF.  batch + spatial (channel,height,width)");
		if( output.length(1) != input.getNumBands() )
			throw new IllegalArgumentException("Number of bands don't match");
		if( output.length(2) != input.getHeight() )
			throw new IllegalArgumentException("Spatial height doesn't match");
		if( output.length(3) != input.getWidth() )
			throw new IllegalArgumentException("Spatial width doesn't match");

		for (int i = 0; i < input.getNumBands(); i++) {
			GrayF32 band = input.getBand(i);
			int indexOut = output.idx(miniBatch,i,0,0);

			int length = input.width*input.height;
			System.arraycopy(band.data, 0,output.d,indexOut,length);
		}
	}

	public static Planar<GrayF32> tensorToImage( Tensor_F32 input , Planar<GrayF32> output , int miniBatch ) {
		if( input.getDimension() != 4 )
			throw new IllegalArgumentException("Input should be 4-DOF.  batch + spatial (channel,height,width)");

		int bands = input.length(1);
		int height = input.length(2);
		int width = input.length(3);

		if( output == null ) {
			output = new Planar<>(GrayF32.class,width,height,bands);
		} else {
			if (input.length(1) != output.getNumBands())
				throw new IllegalArgumentException("Number of bands don't match");
			if (input.length(2) != output.getHeight())
				throw new IllegalArgumentException("Spatial height doesn't match");
			if (input.length(3) != output.getWidth())
				throw new IllegalArgumentException("Spatial width doesn't match");
		}
		for (int i = 0; i < bands; i++) {
			int indexIn = input.idx(miniBatch,i,0,0);
			GrayF32 band = output.getBand(i);

			int length = output.width*output.height;
			System.arraycopy(input.d,indexIn,band.data,0,length);
		}

		return output;
	}
}
