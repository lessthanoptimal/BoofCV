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

package boofcv.alg.misc;

import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

/**
 * Collection of functions that project Bands of Planar images onto
 * a single image. Can be used to perform projections such as
 * minimum, maximum, average, median, standard Deviation.
 *
 * @author Nico Stuurman
 */
@SuppressWarnings("unchecked")
public class GImageBandMath {

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing minimum pixel values
	 */
	public static <T extends ImageGray<T>> void minimum( Planar<T> input, T output ) {
		minimum(input, output, 0, input.getNumBands() - 1);
	}

	/**
	 * Computes the minimum for each pixel across specified bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing minimum pixel values
	 * @param startBand First band to be considered
	 * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void minimum( Planar<T> input, T output, int startBand, int lastBand ) {

		if (GrayU8.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayU8>)input, (GrayU8)output, startBand, lastBand);
		} else if (GrayU16.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayU16>)input, (GrayU16)output, startBand, lastBand);
		} else if (GrayS16.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayS16>)input, (GrayS16)output, startBand, lastBand);
		} else if (GrayS32.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayS32>)input, (GrayS32)output, startBand, lastBand);
		} else if (GrayS64.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayS64>)input, (GrayS64)output, startBand, lastBand);
		} else if (GrayF32.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayF32>)input, (GrayF32)output, startBand, lastBand);
		} else if (GrayF64.class == input.getBandType()) {
			ImageBandMath.minimum((Planar<GrayF64>)input, (GrayF64)output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: " + input.getBandType().getSimpleName());
		}
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageGray<T>> void maximum( Planar<T> input, T output ) {
		maximum(input, output, 0, input.getNumBands() - 1);
	}

	/**
	 * Computes the maximum for each pixel across the specified bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 * @param startBand First band to be considered
	 * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void maximum( Planar<T> input, T output, int startBand, int lastBand ) {

		if (GrayU8.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayU8>)input, (GrayU8)output, startBand, lastBand);
		} else if (GrayU16.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayU16>)input, (GrayU16)output, startBand, lastBand);
		} else if (GrayS16.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayS16>)input, (GrayS16)output, startBand, lastBand);
		} else if (GrayS32.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayS32>)input, (GrayS32)output, startBand, lastBand);
		} else if (GrayS64.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayS64>)input, (GrayS64)output, startBand, lastBand);
		} else if (GrayF32.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayF32>)input, (GrayF32)output, startBand, lastBand);
		} else if (GrayF64.class == input.getBandType()) {
			ImageBandMath.maximum((Planar<GrayF64>)input, (GrayF64)output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: " + input.getBandType().getSimpleName());
		}
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageGray<T>> void average( Planar<T> input, T output ) {
		average(input, output, 0, input.getNumBands() - 1);
	}

	/**
	 * Computes the average for each pixel across the specified bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 * @param startBand First band to be considered
	 * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void average( Planar<T> input, T output, int startBand, int lastBand ) {

		if (GrayU8.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayU8>)input, (GrayU8)output, startBand, lastBand);
		} else if (GrayU16.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayU16>)input, (GrayU16)output, startBand, lastBand);
		} else if (GrayS16.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayS16>)input, (GrayS16)output, startBand, lastBand);
		} else if (GrayS32.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayS32>)input, (GrayS32)output, startBand, lastBand);
		} else if (GrayS64.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayS64>)input, (GrayS64)output, startBand, lastBand);
		} else if (GrayF32.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayF32>)input, (GrayF32)output, startBand, lastBand);
		} else if (GrayF64.class == input.getBandType()) {
			ImageBandMath.average((Planar<GrayF64>)input, (GrayF64)output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: " + input.getBandType().getSimpleName());
		}
	}

	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
	 */
	public static <T extends ImageGray<T>> void median( Planar<T> input, T output ) {
		median(input, output, 0, input.getNumBands() - 1);
	}

	/**
	 * Computes the median for each pixel across the specified bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
	 * @param startBand First band to be considered
	 * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void median( Planar<T> input, T output, int startBand, int lastBand ) {

		if (GrayU8.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayU8>)input, (GrayU8)output, startBand, lastBand);
		} else if (GrayU16.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayU16>)input, (GrayU16)output, startBand, lastBand);
		} else if (GrayS16.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayS16>)input, (GrayS16)output, startBand, lastBand);
		} else if (GrayS32.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayS32>)input, (GrayS32)output, startBand, lastBand);
		} else if (GrayS64.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayS64>)input, (GrayS64)output, startBand, lastBand);
		} else if (GrayF32.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayF32>)input, (GrayF32)output, startBand, lastBand);
		} else if (GrayF64.class == input.getBandType()) {
			ImageBandMath.median((Planar<GrayF64>)input, (GrayF64)output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: " + input.getBandType().getSimpleName());
		}
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 * @param avg Gray scale image containing average projection of input
	 */
	public static <T extends ImageGray<T>> void stdDev( Planar<T> input, T output, @Nullable T avg ) {
		stdDev(input, output, avg, 0, input.getNumBands() - 1);
	}

	/**
	 * Computes the standard deviation for each pixel across the specified bands in the {@link Planar} image.
	 *
	 * @param input Planar image - unchanged
	 * @param output Output Gray scale Image - changed
	 * @param avg Optional gray scale image containing average pixel values - should be same size as a plane in input
	 * @param startBand First band to be considered
	 * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void stdDev( Planar<T> input, T output, @Nullable T avg, int startBand, int lastBand ) {

		if (GrayU8.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayU8>)input, (GrayU8)output, (GrayU8)avg, startBand, lastBand);
		} else if (GrayU16.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayU16>)input, (GrayU16)output, (GrayU16)avg, startBand, lastBand);
		} else if (GrayS16.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayS16>)input, (GrayS16)output, (GrayS16)avg, startBand, lastBand);
		} else if (GrayS32.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayS32>)input, (GrayS32)output, (GrayS32)avg, startBand, lastBand);
		} else if (GrayS64.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayS64>)input, (GrayS64)output, (GrayS64)avg, startBand, lastBand);
		} else if (GrayF32.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayF32>)input, (GrayF32)output, (GrayF32)avg, startBand, lastBand);
		} else if (GrayF64.class == input.getBandType()) {
			ImageBandMath.stdDev((Planar<GrayF64>)input, (GrayF64)output, (GrayF64)avg, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: " + input.getBandType().getSimpleName());
		}
	}
}
