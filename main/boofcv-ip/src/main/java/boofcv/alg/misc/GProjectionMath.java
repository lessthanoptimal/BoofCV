/* 
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

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * Collection of functions that project Bands of Planar images onto 
 * a single image. Can be used to perform projections such as 
 * minimum, maximum, average, median, standard Deviation.
 * 
 * 
 * @author Nico
 */
public class GProjectionMath {
   
   /** Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageGray<T>> void minimumBand(Planar<T> input, T output) {
      minimumBand(input, output, 0, input.getNumBands() - 1);
   }

   
   
   /**
	 * Computes the minimum for each pixel across specified bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be considered
    * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void minimumBand(Planar<T> input, T output, int startBand, int lastBand) {

		if( GrayU8.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayU8>) input, (GrayU8) output, startBand, lastBand);
		} else if( GrayS8.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayS8>) input, (GrayS8) output, startBand, lastBand);
		} else if( GrayU16.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayU16>) input, (GrayU16) output, startBand, lastBand);
		} else if( GrayS16.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayS16>) input, (GrayS16) output, startBand, lastBand);
		} else if( GrayS32.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayS32>) input, (GrayS32) output, startBand, lastBand);
		} else if( GrayS64.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayS64>) input, (GrayS64) output, startBand, lastBand);
		} else if( GrayF32.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayF32>) input, (GrayF32) output, startBand, lastBand);
		} else if( GrayF64.class == input.getBandType() ) {
			ProjectionMath.minimumBand((Planar<GrayF64>) input, (GrayF64) output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getBandType().getSimpleName());
		}
	}
   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageGray<T>> void maximumBand(Planar<T> input, T output) {
      maximumBand(input, output, 0, input.getNumBands() - 1);
   }
   
   /**
	 * Computes the maximum for each pixel across the specified bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be considered
    * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void maximumBand(Planar<T> input, T output, int startBand, int lastBand) {

		if( GrayU8.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayU8>) input, (GrayU8) output, startBand, lastBand);
		} else if( GrayS8.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayS8>) input, (GrayS8) output, startBand, lastBand);
		} else if( GrayU16.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayU16>) input, (GrayU16) output, startBand, lastBand);
		} else if( GrayS16.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayS16>) input, (GrayS16) output, startBand, lastBand);
		} else if( GrayS32.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayS32>) input, (GrayS32) output, startBand, lastBand);
		} else if( GrayS64.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayS64>) input, (GrayS64) output, startBand, lastBand);
		} else if( GrayF32.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayF32>) input, (GrayF32) output, startBand, lastBand);
		} else if( GrayF64.class == input.getBandType() ) {
			ProjectionMath.maximumBand((Planar<GrayF64>) input, (GrayF64) output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getBandType().getSimpleName());
		}
	}
   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageGray<T>> void averageBand(Planar<T> input, T output) {
      averageBand(input, output, 0, input.getNumBands() - 1);
   }
   
   /**
	 * Computes the average for each pixel across the specified bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be considered
    * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void averageBand(Planar<T> input, T output, int startBand, int lastBand) {

		if( GrayU8.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayU8>) input, (GrayU8) output, startBand, lastBand);
		} else if( GrayS8.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayS8>) input, (GrayS8) output, startBand, lastBand);
		} else if( GrayU16.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayU16>) input, (GrayU16) output, startBand, lastBand);
		} else if( GrayS16.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayS16>) input, (GrayS16) output, startBand, lastBand);
		} else if( GrayS32.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayS32>) input, (GrayS32) output, startBand, lastBand);
		} else if( GrayS64.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayS64>) input, (GrayS64) output, startBand, lastBand);
		} else if( GrayF32.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayF32>) input, (GrayF32) output, startBand, lastBand);
		} else if( GrayF64.class == input.getBandType() ) {
			ProjectionMath.averageBand((Planar<GrayF64>) input, (GrayF64) output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getBandType().getSimpleName());
		}
	}
   
   
    /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
	 */
	public static <T extends ImageGray<T>> void medianBand(Planar<T> input, T output) {
      medianBand(input, output, 0, input.getNumBands() - 1);
   }
   
   /**
	 * Computes the median for each pixel across the specified bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be considered
    * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void medianBand(Planar<T> input, T output, int startBand, int lastBand) {

		if( GrayU8.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayU8>) input, (GrayU8) output, startBand, lastBand);
		} else if( GrayS8.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayS8>) input, (GrayS8) output, startBand, lastBand);
		} else if( GrayU16.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayU16>) input, (GrayU16) output, startBand, lastBand);
		} else if( GrayS16.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayS16>) input, (GrayS16) output, startBand, lastBand);
		} else if( GrayS32.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayS32>) input, (GrayS32) output, startBand, lastBand);
		} else if( GrayS64.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayS64>) input, (GrayS64) output, startBand, lastBand);
		} else if( GrayF32.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayF32>) input, (GrayF32) output, startBand, lastBand);
		} else if( GrayF64.class == input.getBandType() ) {
			ProjectionMath.medianBand((Planar<GrayF64>) input, (GrayF64) output, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getBandType().getSimpleName());
		}
	}

   
   /**
	 * Computes the standard deviation for each pixel across all bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param avg Gray scale image containing average projection of input
	 */
	public static <T extends ImageGray<T>> void stdDevBand(Planar<T> input, T output, T avg) {
      stdDevBand(input, output, avg, 0, input.getNumBands() - 1);
   }
   
   /**
	 * Computes the standard deviation for each pixel across the specified bands in the {@link Planar} image.
	 *
    * @param <T>
	 * @param input Planar image - unchanged
	 * @param output Gray scale image containing average pixel values
    * @param avg Gray scali
    * @param startBand First band to be considered
    * @param lastBand Last band (inclusive) to be considered
	 */
	public static <T extends ImageGray<T>> void stdDevBand(Planar<T> input, T output, T avg, int startBand, int lastBand) {

		if( GrayU8.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayU8>) input, (GrayU8) output, (GrayU8) avg, startBand, lastBand);
		} else if( GrayS8.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayS8>) input, (GrayS8) output, (GrayS8) avg, startBand, lastBand);
		} else if( GrayU16.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayU16>) input, (GrayU16) output, (GrayU16) avg, startBand, lastBand);
		} else if( GrayS16.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayS16>) input, (GrayS16) output, (GrayS16) avg, startBand, lastBand);
		} else if( GrayS32.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayS32>) input, (GrayS32) output, (GrayS32) avg, startBand, lastBand);
		} else if( GrayS64.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayS64>) input, (GrayS64) output, (GrayS64) avg, startBand, lastBand);
		} else if( GrayF32.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayF32>) input, (GrayF32) output, (GrayF32) avg, startBand, lastBand);
		} else if( GrayF64.class == input.getBandType() ) {
			ProjectionMath.stdDevBand((Planar<GrayF64>) input, (GrayF64) output, (GrayF64) avg, startBand, lastBand);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getBandType().getSimpleName());
		}
	}
}

