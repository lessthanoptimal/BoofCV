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

import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;
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
 * @see CensusTransform
 *
 * @author Peter Abeles
 */
public class GCensusTransform {
	public static<T extends ImageGray<T>> void dense3x3(final T input , final GrayU8 output ,
														@Nullable ImageBorder<T> border )
	{
		if( input.getClass() == GrayU8.class ) {
			CensusTransform.dense3x3((GrayU8)input,output,(ImageBorder_S32)border);
		} else if( input.getClass() == GrayU16.class ) {
			CensusTransform.dense3x3((GrayU16)input,output,(ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type. "+input.getClass().getSimpleName());
		}
	}

	public static<T extends ImageGray<T>> void dense5x5(final T input , final GrayS32 output ,
														@Nullable ImageBorder<T> border )
	{
		if( input.getClass() == GrayU8.class ) {
			CensusTransform.dense5x5((GrayU8)input,output,(ImageBorder_S32)border);
		} else if( input.getClass() == GrayU16.class ) {
			CensusTransform.dense5x5((GrayU16)input,output,(ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type. "+input.getClass().getSimpleName());
		}

	}

	public static<T extends ImageGray<T>> void sample_S64(final T input , final FastAccess<Point2D_I32> sample,
														  final GrayS64 output ,
														  @Nullable ImageBorder<T> border, @Nullable GrowQueue_I32 workSpace )
	{
		if( input.getClass() == GrayU8.class ) {
			CensusTransform.sample_S64((GrayU8)input,sample,output,(ImageBorder_S32)border,workSpace);
		} else if( input.getClass() == GrayU16.class ) {
			CensusTransform.sample_S64((GrayU16)input,sample,output,(ImageBorder_S32)border,workSpace);
		} else {
			throw new IllegalArgumentException("Unknown input image type. "+input.getClass().getSimpleName());
		}

	}

	public static<T extends ImageGray<T>> void sample_IU16(final T input , final FastAccess<Point2D_I32> sample,
														   final InterleavedU16 output ,
														   @Nullable ImageBorder<T> border, @Nullable GrowQueue_I32 workSpace )
	{
		if( input.getClass() == GrayU8.class ) {
			CensusTransform.sample_IU16((GrayU8)input,sample,output,(ImageBorder_S32)border,workSpace);
		} else if( input.getClass() == GrayU16.class ) {
			CensusTransform.sample_IU16((GrayU16)input,sample,output,(ImageBorder_S32)border,workSpace);
		} else {
			throw new IllegalArgumentException("Unknown input image type. "+input.getClass().getSimpleName());
		}

	}

}
