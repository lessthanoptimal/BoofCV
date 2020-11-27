/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.enhance;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * Weakly typed version of {@link EnhanceImageOps}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class GEnhanceImageOps {
	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param minValue Minimum possible pixel value.
	 * @param output Output image.
	 */
	public static <T extends ImageGray<T>>
	void applyTransform( T input, int[] transform, int minValue, T output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (input instanceof GrayU8) {
			EnhanceImageOps.applyTransform((GrayU8)input, transform, (GrayU8)output);
		} else if (input instanceof GrayS8) {
			EnhanceImageOps.applyTransform((GrayS8)input, transform, minValue, (GrayS8)output);
		} else if (input instanceof GrayU16) {
			EnhanceImageOps.applyTransform((GrayU16)input, transform, (GrayU16)output);
		} else if (input instanceof GrayS16) {
			EnhanceImageOps.applyTransform((GrayS16)input, transform, minValue, (GrayS16)output);
		} else if (input instanceof GrayS32) {
			EnhanceImageOps.applyTransform((GrayS32)input, transform, minValue, (GrayS32)output);
		} else {
			throw new IllegalArgumentException("Image type not supported. " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Equalizes the local image histogram on a per pixel basis.
	 *
	 * @param input Input image.
	 * @param radius Radius of square local histogram.
	 * @param output Output image.
	 * @param histogramLength Number of elements in the histogram. 256 for 8-bit images
	 * @param workArrays Used to create work arrays. can be null
	 */
	public static <T extends ImageBase<T>>
	void equalizeLocal( T input, int radius, T output,
						int histogramLength, @Nullable GrowArray<DogArray_I32> workArrays ) {
		if (input instanceof Planar) {
			Planar pi = (Planar)input;
			Planar po = (Planar)output;
			for (int i = 0; i < pi.getNumBands(); i++) {
				equalizeLocal(pi.getBand(i), radius, po.getBand(i), histogramLength, workArrays);
			}
		} else {
			if (input instanceof GrayU8) {
				EnhanceImageOps.equalizeLocal((GrayU8)input, radius, (GrayU8)output, histogramLength, workArrays);
			} else if (input instanceof GrayU16) {
				EnhanceImageOps.equalizeLocal((GrayU16)input, radius, (GrayU16)output, histogramLength, workArrays);
			} else {
				throw new IllegalArgumentException("Unsupported image type " + input.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static <T extends ImageBase<T>> void sharpen4( T input, T output ) {
		if (input instanceof Planar) {
			Planar pi = (Planar)input;
			Planar po = (Planar)output;
			for (int i = 0; i < pi.getNumBands(); i++) {
				sharpen4(pi.getBand(i), po.getBand(i));
			}
		} else {
			if (input instanceof GrayU8) {
				EnhanceImageOps.sharpen4((GrayU8)input, (GrayU8)output);
			} else if (input instanceof GrayF32) {
				EnhanceImageOps.sharpen4((GrayF32)input, (GrayF32)output);
			} else {
				throw new IllegalArgumentException("Image type not supported. " + input.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static <T extends ImageBase<T>> void sharpen8( T input, T output ) {
		if (input instanceof Planar) {
			Planar pi = (Planar)input;
			Planar po = (Planar)output;
			for (int i = 0; i < pi.getNumBands(); i++) {
				sharpen8(pi.getBand(i), po.getBand(i));
			}
		} else {
			if (input instanceof GrayU8) {
				EnhanceImageOps.sharpen8((GrayU8)input, (GrayU8)output);
			} else if (input instanceof GrayF32) {
				EnhanceImageOps.sharpen8((GrayF32)input, (GrayF32)output);
			} else {
				throw new IllegalArgumentException("Image type not supported. " + input.getClass().getSimpleName());
			}
		}
	}
}
