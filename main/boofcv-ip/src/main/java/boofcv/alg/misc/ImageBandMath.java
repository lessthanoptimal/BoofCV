/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.impl.ImplImageBandMath;
import boofcv.struct.image.*;

/**
 * Collection of functions that project Bands of Planar images onto
 * a single image. Can be used to perform projections such as
 * minimum, maximum, average, median, standard Deviation.
 *
 * @author Nico
 */
@SuppressWarnings("Duplicates")
public class ImageBandMath {
	// TODO create a generator for this class

	// Note: I removed support for GrayS8 since I never use that data type. Can you think of a use?
	// TODO parameter order should be all inputs then output
	//    input, start, last, output ... etc
	// TODO reshape output to match input
	// TODO make ImplImageBandMath match this class. I recommend using a refactoring tool first then changing the generator
	// TODO add @Nullable to avg in stdev

	public static <T extends ImageGray<T>> void checkInput(Planar<T> input, int startBand, int lastBand) {
		if (startBand < 0 || lastBand < 0) {
			throw new IllegalArgumentException("startBand or lastBand is less than zero");
		}
		if (startBand > lastBand) {
			throw new IllegalArgumentException("startBand should <= lastBand");
		}
		if (lastBand >= input.getNumBands()) {
			throw new IllegalArgumentException("lastBand should be less than number of Bands in input");
		}
	}

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input, output, startBand, lastBand);
	}

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayU16> input, GrayU16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input, output, startBand, lastBand);
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayS16> input, GrayS16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayS32> input, GrayS32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input,output,startBand,lastBand);
	}


	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayS64> input, GrayS64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayF32> input, GrayF32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing minimum pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void minimum(Planar<GrayF64> input, GrayF64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.minimum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayU16> input, GrayU16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}


	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayS16> input, GrayS16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayS32> input, GrayS32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayS64> input, GrayS64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}


	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayF32> input, GrayF32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}

	/**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing minimum pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void maximum(Planar<GrayF64> input, GrayF64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.maximum(input,output,startBand,lastBand);
	}


	/**
	 * Computes the average for each pixel across all bands in the {@link Planar}
	 * image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayU16> input, GrayU16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayS16> input, GrayS16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayS32> input, GrayS32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}


	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayS64> input, GrayS64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}


	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayF32> input, GrayF32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing average pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void average(Planar<GrayF64> input, GrayF64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.average(input,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayU8> input, GrayU8 avg, GrayU8 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayU8(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayU16> input, GrayU16 avg, GrayU16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayU16(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}


	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayS16> input, GrayS16 avg, GrayS16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayS16(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayS32> input, GrayS32 avg, GrayS32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayS32(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayS64> input, GrayS64 avg, GrayS64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayS64(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayF32> input, GrayF32 avg, GrayF32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayF32(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar}
	 * image.
	 * @param input     Planar image - not modified
	 * @param avg       Input Gray scale image containing average image.  Can be null
	 * @param output    Gray scale image containing average pixel values - modified
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void stdDev(Planar<GrayF64> input, GrayF64 avg, GrayF64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		if( avg == null ) {
			avg = new GrayF64(input.width,input.height);
			average(input,avg,startBand,lastBand);
		}
		ImplImageBandMath.stdDev(input,avg,output,startBand,lastBand);
	}

	/**
	 * Computes the median for each pixel across all bands in the {@link Planar}
	 * image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}

	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayU16> input, GrayU16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}


	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayS16> input, GrayS16 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}

	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayS32> input, GrayS32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}


	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayS64> input, GrayS64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}


	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayF32> input, GrayF32 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}

	/**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
	 * @param input     Planar image
	 * @param output    Gray scale image containing median pixel values
	 * @param startBand First band to be included in the projection
	 * @param lastBand  Last band to be included in the projection
	 */
	public static void median(Planar<GrayF64> input, GrayF64 output, int startBand, int lastBand) {
		checkInput(input, startBand, lastBand);
		ImplImageBandMath.median(input,output,startBand,lastBand);
	}


}

