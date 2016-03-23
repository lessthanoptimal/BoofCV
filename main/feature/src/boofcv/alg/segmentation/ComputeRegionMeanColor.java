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

package boofcv.alg.segmentation;

import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Computes the mean color for regions in a segmented image.
 *
 * @author Peter Abeles
 */
public abstract class ComputeRegionMeanColor<T extends ImageBase> {

	// Input image
	T image;
	// Number of bands in the input image
	final int numBands;

	// storage for the sum of each color
	FastQueue<float[]> regionSums;

	/**
	 * Constructor
	 *
	 * @param numBands Number of bands in the color image
	 */
	public ComputeRegionMeanColor(final int numBands) {
		this.numBands = numBands;

		regionSums = new ColorQueue_F32(numBands);
	}

	/**
	 * Compute the average color for each region
	 *
	 * @param image Input image
	 * @param pixelToRegion Conversion between pixel to region index
	 * @param regionMemberCount List which stores the number of members for each region
	 * @param regionColor (Output) Storage for mean color throughout the region.  Internal array must be fully
	 *                    declared.
	 */
	public void process( T image , GrayS32 pixelToRegion ,
						 GrowQueue_I32 regionMemberCount ,
						 FastQueue<float[]> regionColor  )  {

		this.image = image;

		// Initialize data structures
		regionSums.resize(regionColor.size);
		for( int i = 0; i < regionSums.size; i++ ) {
			float v[] = regionSums.get(i);
			for( int j = 0; j < v.length; j++ ) {
				v[j] = 0;
			}
		}

		// Sum up the pixel values for each region
		for( int y = 0; y < image.height; y++ ) {
			int indexImg = image.startIndex + y*image.stride;
			int indexRgn = pixelToRegion.startIndex + y*pixelToRegion.stride;

			for( int x = 0; x < image.width; x++ , indexRgn++, indexImg++ ) {
				int region = pixelToRegion.data[indexRgn];
				float[] sum = regionSums.get(region);

				addPixelValue(indexImg,sum);
			}
		}

		// Compute the average using the sum and update the region color
		for( int i = 0; i < regionSums.size; i++ ) {
			float N = regionMemberCount.get(i);
			float[] sum = regionSums.get(i);
			float[] average = regionColor.get(i);

			for( int j = 0; j < numBands; j++ ) {
				average[j] = sum[j]/N;
			}
		}
	}

	/**
	 * Image type specific implementation.  Adds the pixel value at the specified pixel to sum
	 * @param index Pixel index in the image which is being read
	 * @param sum Where the pixel's value is added to
	 */
	protected abstract void addPixelValue(int index , float[] sum );

	/**
	 * Implementation for {@link GrayU8}
	 */
	public static class U8 extends ComputeRegionMeanColor<GrayU8> {
		public U8() {super(1);}

		@Override
		protected void addPixelValue(int index, float[] sum) {
			sum[0] += image.data[index] & 0xFF;
		}
	}

	/**
	 * Implementation for {@link GrayF32}
	 */
	public static class F32 extends ComputeRegionMeanColor<GrayF32> {
		public F32() {super(1);}

		@Override
		protected void addPixelValue(int index, float[] sum) {
			sum[0] += image.data[index];
		}
	}

	/**
	 * Implementation for {@link Planar}
	 */
	public static class PL_U8 extends ComputeRegionMeanColor<Planar<GrayU8>> {
		public PL_U8( int numBands ) {super(numBands);}

		@Override
		protected void addPixelValue(int index, float[] sum) {
			for( int i = 0; i < numBands; i++ ) {
				sum[i] += image.bands[i].data[index] & 0xFF;
			}
		}
	}

	/**
	 * Implementation for {@link Planar}
	 */
	public static class PL_F32 extends ComputeRegionMeanColor<Planar<GrayF32>> {
		public PL_F32( int numBands ) {super(numBands);}

		@Override
		protected void addPixelValue(int index, float[] sum) {
			for( int i = 0; i < numBands; i++ ) {
				sum[i] += image.bands[i].data[index];
			}
		}
	}
}
