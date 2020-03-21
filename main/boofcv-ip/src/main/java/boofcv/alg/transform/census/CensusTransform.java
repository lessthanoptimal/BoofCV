/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.transform.census.impl.ImplCensusTransformBorder;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import javax.annotation.Nullable;

/**
 * <p>The Census Transform [1] computes a bit mask for each pixel in the image. If a neighboring pixel is greater than the
 * center pixel in a region that bit is set to 1. A 3x3 region 9radius=1) is encoded in 8-bits and a 5x5 region
 * (radius=2) in 24-bits. To compute the error between two pixels simply compute the hamming distance. The
 * hamming distance for an input can be computed using DescriptorDistance.hamming().</p>
 *
 * <p>DEVELOPMENT NOTE: See if this can be speed up by only comparing each pixel with another once.
 * Code will be complex</p>
 *
 * <p>
 * [1] Zabih, Ramin, and John Woodfill. "Non-parametric local transforms for computing visual correspondence."
 * European conference on computer vision. Springer, Berlin, Heidelberg, 1994.
 * </p>
 *
 * @author Peter Abeles
 */
public class CensusTransform {

	public static FastQueue<Point2D_I32> createBlockSamples( int radius ) {
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32::new);
		int w = radius*2+1;
		samples.growArray(w*w-1);

		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				if( x == 0 && y == 0) // don't sample the center
					continue;
				samples.grow().set(x,y);
			}
		}

		return samples;
	}

	public static FastQueue<Point2D_I32> createBlockSamples(int radiusX , int radiusY) {
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32::new);
		int wx = radiusX*2+1;
		int wy = radiusY*2+1;
		samples.growArray(wx*wy-1);

		for (int y = -radiusY; y <= radiusY; y++) {
			for (int x = -radiusX; x <= radiusX; x++) {
				if( x == 0 && y == 0) // don't sample the center
					continue;
				samples.grow().set(x,y);
			}
		}

		return samples;
	}

	public static FastQueue<Point2D_I32> createCircleSamples(){
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32::new);
		for (int row = 0; row < 9; row++) {
			int col0 = row <= 4 ? Math.max(0,3-row) : row-5;
			int col1 = 9-col0;
			for (int col = col0; col < col1; col++) {
				if( row == 4 && col == 4 )
					continue;
				samples.grow().set(row-4,col-4);
			}
		}
		return samples;
	}

	/**
	 * Census transform for local 3x3 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense3x3(final GrayU8 input , final GrayU8 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense3x3(input,output);
		} else {
			ImplCensusTransformInner.dense3x3(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense3x3_U8(border,output);
		}
	}

	/**
	 * Census transform for local 3x3 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense3x3(final GrayU16 input , final GrayU8 output , @Nullable ImageBorder_S32<GrayU16> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense3x3(input,output);
		} else {
			ImplCensusTransformInner.dense3x3(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense3x3_U8(border,output);
		}
	}


	/**
	 * Census transform for local 5x5 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense5x5(final GrayU8 input , final GrayS32 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense5x5(input,output);
		} else {
			ImplCensusTransformInner.dense5x5(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense5x5_U8(border,output);
		}
	}

	/**
	 * Census transform for local 5x5 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense5x5(final GrayU16 input , final GrayS32 output , @Nullable ImageBorder_S32<GrayU16> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense5x5(input,output);
		} else {
			ImplCensusTransformInner.dense5x5(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense5x5_U8(border,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_S64(final GrayU8 input , final FastAccess<Point2D_I32> sample,
								 final GrayS64 output , @Nullable ImageBorder_S32<GrayU8> border ,
								 @Nullable GrowQueue_I32 workSpace ) {
		output.reshape(input.width,input.height);

		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();

		int borderRadius = computeRadiusWorkspace(input, sample, workSpace);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_S64(input,borderRadius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_S64(input,borderRadius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_S64(border,borderRadius,sample,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_S64(final GrayU16 input , final FastAccess<Point2D_I32> sample,
								  final GrayS64 output , @Nullable ImageBorder_S32<GrayU16> border ,
								  @Nullable GrowQueue_I32 workSpace ) {
		output.reshape(input.width,input.height);

		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();

		int borderRadius = computeRadiusWorkspace(input, sample, workSpace);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_S64(input,borderRadius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_S64(input,borderRadius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_S64(border,borderRadius,sample,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_IU16(final GrayU8 input , final FastAccess<Point2D_I32> sample,
								   final InterleavedU16 output , @Nullable ImageBorder_S32<GrayU8> border ,
								   @Nullable GrowQueue_I32 workSpace ) {
		// Compute the number of 16-bit values that are needed to store
		int numBlocks = BoofMiscOps.bitsToWords(sample.size,16);
		output.reshape(input.width,input.height,numBlocks);

		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();

		int borderRadius = computeRadiusWorkspace(input, sample, workSpace);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_IU16(input,borderRadius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_IU16(input,borderRadius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_IU16(border,borderRadius,sample,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_IU16(final GrayU16 input , final FastAccess<Point2D_I32> sample,
								   final InterleavedU16 output , @Nullable ImageBorder_S32<GrayU16> border ,
								   @Nullable GrowQueue_I32 workSpace ) {
		// Compute the number of 16-bit values that are needed to store
		int numBlocks = BoofMiscOps.bitsToWords(sample.size,16);
		output.reshape(input.width,input.height,numBlocks);

		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();

		int borderRadius = computeRadiusWorkspace(input, sample, workSpace);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_IU16(input,borderRadius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_IU16(input,borderRadius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_IU16(border,borderRadius,sample,output);
		}
	}

	/**
	 *
	 * @param input (Input) Input image
	 * @param sample (Input) Points being sampled
	 * @param workSpace (Output) Stores the offsets from current point that need to be sampled
	 * @return The maximum distance away (x and y) that a point is sampled
	 */
	private static int computeRadiusWorkspace(ImageBase input, FastAccess<Point2D_I32> sample, GrowQueue_I32 workSpace) {
		int radius = 0;
		workSpace.resize(sample.size);
		for (int i = 0; i < sample.size; i++) {
			Point2D_I32 p = sample.get(i);
			workSpace.data[i] = p.y*input.stride + p.x;
			radius = Math.max(radius,Math.abs(p.x));
			radius = Math.max(radius,Math.abs(p.y));
		}
		return radius;
	}
}
