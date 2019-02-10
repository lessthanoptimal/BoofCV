/**
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
import org.ddogleg.sorting.QuickSelect;

/**
 * Collection of functions that project Bands of Planar images onto 
 * a single image. Can be used to perform projections such as 
 * minimum, maximum, average, median, standard Deviation.
 * 
 * 
 * 
 * @author Nico
 */
public class ProjectionMath {
   
   public static <T extends ImageGray<T>> void checkInput (Planar<T> input, int startBand, int lastBand) {
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
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayU8> input , GrayU8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
      
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU8[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int minimum = Byte.MAX_VALUE & 0xFF;
				for( int i = startBand; i <= lastBand; i++ ) {
					if ( (bands[i].data[ indexInput ] & 0xFF) < minimum) {
                  minimum =  (bands[i].data[ indexInput ] & 0xFF);
               }
				}
				output.data[indexOutput] = (byte) minimum;
			}
		}
	}
   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayS8> input , GrayS8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS8[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				byte minimum = Byte.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
					if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = minimum;
			}
		}
	}
   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayU16> input , GrayU16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand); 
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int minimum = Short.MAX_VALUE & 0xFFFF;
				for( int i = startBand; i <= lastBand; i++ ) {
					if ( (bands[i].data[ indexInput ] & 0xFFFF) < minimum) {
                  minimum = (bands[i].data[ indexInput ] & 0xFFFF);
               }
				}
				output.data[indexOutput] = (short) minimum;
			}
		}
	}

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayS16> input , GrayS16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				short minimum = Short.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
					if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = minimum;
			}
		}
	}
   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayS32> input , GrayS32 output,  int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int minimum = Integer.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
            }
				output.data[indexOutput] = minimum;
			}
		}
	}

   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayS64> input , GrayS64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				long minimum = Long.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = minimum;
			}
		}
	}

   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayF32> input , GrayF32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				float minimum = Float.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = minimum;
			}
		}
	}
   
   /**
	 * Computes the minimum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing minimum pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void minimumBand(Planar<GrayF64> input , GrayF64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				double minimum = Double.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] < minimum) {
                  minimum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = minimum;
			}
		}
	}

   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayU8> input , GrayU8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU8[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int maximum = (-Byte.MAX_VALUE);
				for( int i = startBand; i <= lastBand; i++ ) {
					if ( (bands[i].data[ indexInput ]) > maximum) {
                  maximum =  (bands[i].data[ indexInput ]);
               }
				}
				output.data[indexOutput] = (byte) maximum;
			}
		}
	}
   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayS8> input , GrayS8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS8[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				byte maximum = -Byte.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
					if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = maximum;
			}
		}
	}
   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayU16> input , GrayU16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int maximum = -Short.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
					if ( bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = (short) maximum;
			}
		}
	}

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayS16> input , GrayS16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				short maximum = -Short.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
					if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = maximum;
			}
		}
	}
   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayS32> input , GrayS32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				int maximum = -Integer.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
            }
				output.data[indexOutput] = maximum;
			}
		}
	}

   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayS64> input , GrayS64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				long maximum = -Long.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = maximum;
			}
		}
	}

   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayF32> input , GrayF32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				float maximum = -Float.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = maximum;
			}
		}
	}
   
   /**
	 * Computes the maximum for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing minimum pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void maximumBand(Planar<GrayF64> input , GrayF64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				double maximum = -Double.MAX_VALUE;
				for( int i = startBand; i <= lastBand; i++ ) {
               if (bands[i].data[ indexInput ] > maximum) {
                  maximum = bands[i].data[ indexInput ];
               }
				}
				output.data[indexOutput] = maximum;
			}
		}
	}
   
   
   /**
    * Computes the average for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image
    * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
   public static void averageBand(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
      final int h = input.getHeight();
      final int w = input.getWidth();

      GrayU8[] bands = input.bands;

      for (int y = 0; y < h; y++) {
         int indexInput = input.getStartIndex() + y * input.getStride();
         int indexOutput = output.getStartIndex() + y * output.getStride();

         int indexEnd = indexInput + w;
         long sum;
         for (; indexInput < indexEnd; indexInput++, indexOutput++) {
            sum = 0;
            for (int i = startBand; i <= lastBand; i++) {

               sum += (bands[i].data[indexInput] & 0xFF);
            }
            output.data[indexOutput] = (byte) (sum /(lastBand + 1 - startBand));
         }
      }
   }

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayS8> input , GrayS8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS8[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
         long sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
					sum += bands[i].data[ indexInput ];
				}
				output.data[indexOutput] =  (byte) (sum / (lastBand + 1 - startBand));
			}
		}
	}
   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayU16> input , GrayU16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
         long sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
					sum += (bands[i].data[ indexInput ] & 0xFFFF);
				}
				output.data[indexOutput] = (short) (sum / (lastBand + 1 - startBand));
			}
		}
	}

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayS16> input , GrayS16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS16[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
         long sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
					sum += bands[i].data[ indexInput ];
				}
				output.data[indexOutput] = (short) (sum / (lastBand + 1 - startBand));
			}
		}
	}
   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayS32> input , GrayS32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
         long sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
               sum += bands[i].data[ indexInput ];
            }
				output.data[indexOutput] = (int) (sum / (lastBand + 1 - startBand));
			}
		}
	}

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayS64> input , GrayS64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			double sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               sum += bands[i].data[ indexInput ];
				}
				output.data[indexOutput] = (long) (sum / (lastBand + 1 - startBand));
			}
		}
	}

   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayF32> input , GrayF32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF32[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			double sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               sum += bands[i].data[ indexInput ];
				}
				output.data[indexOutput] = (float) (sum / (lastBand + 1 - startBand));
			}
		}
	}
   
   /**
	 * Computes the average for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing average pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void averageBand(Planar<GrayF64> input , GrayF64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF64[] bands = input.bands;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			double sum; 
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               sum += bands[i].data[ indexInput ];
				}
				output.data[indexOutput] = (double) (sum / (lastBand + 1 - startBand));
			}
		}
	}
   
   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
   public static void stdDevBand(Planar<GrayU8> input, GrayU8 output, GrayU8 avg, 
           int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
      final int h = input.getHeight();
      final int w = input.getWidth();
      
      GrayU8 localAvg = avg;
      if (localAvg == null) {
         localAvg = new GrayU8(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
      
      GrayU8[] bands = input.bands;

      for (int y = 0; y < h; y++) {
         int indexInput = input.getStartIndex() + y * input.getStride();
         int indexOutput = output.getStartIndex() + y * output.getStride();

         int indexEnd = indexInput + w;
         int numBands = lastBand + 1 - startBand;
         long sum;
         for (; indexInput < indexEnd; indexInput++, indexOutput++) {
            sum = 0;
            for (int i = startBand; i <= lastBand; i++) {
               double diff = (bands[i].data[indexInput] & 0xFF) - (localAvg.data[indexInput] & 0xFF);
               sum += diff * diff;
            }
            output.data[indexOutput] = (byte) Math.sqrt(sum /(numBands - 1));
         }
      }
   }

   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayS8> input , GrayS8 output, GrayS8 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS8[] bands = input.bands;
      GrayS8 localAvg = avg;
		if (localAvg == null) {
         localAvg = new GrayS8(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
      
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
         int numBands = lastBand + 1 - startBand;
         long sum;
         double diff;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = (bands[i].data[indexInput]) - (localAvg.data[indexInput]);
               sum += diff * diff;
            }
            output.data[indexOutput] = (byte) Math.sqrt(sum /(numBands - 1));
         }
		}
	}
   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayU16> input , GrayU16 output, GrayU16 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU16[] bands = input.bands;
      GrayU16  localAvg = avg;
		if (localAvg == null) {
         localAvg = new GrayU16(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			// for(int x = 0; x < w; x++ ) {
         final int numBands = lastBand + 1 - startBand;
         long sum;
         double diff;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = (bands[i].data[ indexInput ] & 0xFFFF) - (localAvg.data[ indexInput ] & 0xFFFF);
					sum += diff * diff;
				}
            output.data[indexOutput] = (short) Math.sqrt(sum /(numBands - 1));
			}
		}
	}

   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayS16> input , GrayS16 output, GrayS16 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS16[] bands = input.bands;
      GrayS16 localAvg = avg; 
		if (localAvg == null) {
         localAvg = new GrayS16(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
      final int numBands = lastBand + 1 - startBand;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
         long sum;
         double diff;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = bands[i].data[ indexInput ] - localAvg.data[ indexInput ];
					sum += diff * diff;
				}
            output.data[indexOutput] = (short) Math.sqrt(sum /(numBands - 1));
			}
		}
	}
   
/**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayS32> input , GrayS32 output, GrayS32 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS32[] bands = input.bands;
      GrayS32 localAvg = avg; 
		if (localAvg == null) {
         localAvg = new GrayS32(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
      final int numBands = lastBand + 1 - startBand;
      long sum;
      double diff;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = bands[i].data[ indexInput ] - localAvg.data[ indexInput ];
               sum += diff * diff;
            }
				output.data[indexOutput] = (int) Math.sqrt(sum / (numBands - 1));
			}
		}
	}

   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayS64> input , GrayS64 output, GrayS64 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS64[] bands = input.bands;
      GrayS64 localAvg = avg;
		if (localAvg == null) {
         localAvg = new GrayS64(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
      double sum;
      double diff;  
      final int numBands = lastBand + 1 - startBand;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = bands[i].data[ indexInput ] - localAvg.data[ indexInput ];
               sum += diff * diff;
				}
				output.data[indexOutput] = (long) Math.sqrt(sum / (numBands - 1));
			}
		}
	}

   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayF32> input , GrayF32 output, GrayF32 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF32[] bands = input.bands;
      GrayF32 localAvg = avg; 
		if (localAvg == null) {
         localAvg = new GrayF32(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
      double sum;
      double diff;  
      final int numBands = lastBand + 1 - startBand;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = bands[i].data[ indexInput ] - localAvg.data[ indexInput ];
               sum += diff * diff;
				}
				output.data[indexOutput] = (float) Math.sqrt(sum / (numBands - 1));
			}
		}
	}
   
   /**
    * Computes the standard deviation for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image - not modified
    * @param output Gray scale image containing average pixel values - modified
    * @param avg Input Gray scale image containing average image.  Can be null
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
	public static void stdDevBand(Planar<GrayF64> input , GrayF64 output, GrayF64 avg, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF64[] bands = input.bands;
      GrayF64 localAvg = avg;
		if (localAvg == null) {
         localAvg = new GrayF64(w, h);
         averageBand(input, localAvg, startBand, lastBand);
      }
		
      double sum;
      double diff;  
      final int numBands = lastBand + 1 - startBand;
		
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               diff = bands[i].data[ indexInput ] - localAvg.data[ indexInput ];
               sum += diff * diff;
				}
				output.data[indexOutput] = (double) Math.sqrt(sum / (numBands - 1));
			}
		}
	}
   
    /**
    * Computes the median for each pixel across all bands in the {@link Planar}
    * image.
    *
    * @param input Planar image
    * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
    */
   public static void medianBand(Planar<GrayU8> input, GrayU8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
      final int h = input.getHeight();
      final int w = input.getWidth();

      GrayU8[] bands = input.bands;
      
      int numBands = lastBand - startBand + 1;
      byte[] valueArray = new byte[numBands];
      boolean isEven = numBands % 2 == 0;
      for (int y = 0; y < h; y++) {
         int indexInput = input.getStartIndex() + y * input.getStride();
         int indexOutput = output.getStartIndex() + y * output.getStride();

         int indexEnd = indexInput + w;
         for (; indexInput < indexEnd; indexInput++, indexOutput++) {
            for (int i = startBand; i <= lastBand; i++) {
               valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] = (byte) ( (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2);
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
      }
   }

   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayS8> input , GrayS8 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS8[] bands = input.bands;
		
      int numBands = lastBand - startBand + 1;
      byte[] valueArray = new byte[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
					valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] = (byte) ( (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2);
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}
   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayU16> input , GrayU16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayU16[] bands = input.bands;
		
      int numBands = lastBand - startBand + 1;
      short[] valueArray = new short[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
               valueArray[i] = bands[i].data[ indexInput];
				}
            if (isEven) {
               output.data[indexOutput] = (short) ( (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2);
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}

   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayS16> input , GrayS16 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS16[] bands = input.bands;
		int numBands = lastBand - startBand + 1;
      short[] valueArray = new short[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
					valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] = (short) ( (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2);
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}
   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayS32> input , GrayS32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS32[] bands = input.bands;
		
      int[] valueArray = new int[lastBand - startBand + 1];
      boolean isEven = valueArray.length % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
               valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] = (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2;
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}

   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayS64> input , GrayS64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayS64[] bands = input.bands;
		int numBands = lastBand - startBand + 1;
      long[] valueArray = new long[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			double sum;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				sum = 0.0;
				for( int i = startBand; i <= lastBand; i++ ) {
               valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] =  (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2;
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}

   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayF32> input , GrayF32 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF32[] bands = input.bands;
		int numBands = lastBand - startBand + 1;
      float[] valueArray = new float[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
               valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] =  (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2;
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}
   
   /**
	 * Computes the median for each pixel across all bands in the {@link Planar} image.
	 * 
	 * @param input Planar image
	 * @param output Gray scale image containing median pixel values
    * @param startBand First band to be included in the projection
    * @param lastBand Last band to be included in the projection
	 */
	public static void medianBand(Planar<GrayF64> input , GrayF64 output, int startBand, int lastBand ) {
      checkInput(input, startBand, lastBand);
		final int h = input.getHeight();
		final int w = input.getWidth();

		GrayF64[] bands = input.bands;
		int numBands = lastBand - startBand + 1;
      double[] valueArray = new double[numBands];
      boolean isEven = numBands % 2 == 0;
		for (int y = 0; y < h; y++) {
			int indexInput = input.getStartIndex() + y * input.getStride();
			int indexOutput = output.getStartIndex() + y * output.getStride();

			int indexEnd = indexInput+w;
			for (; indexInput < indexEnd; indexInput++, indexOutput++ ) {
				for( int i = startBand; i <= lastBand; i++ ) {
               valueArray[i] = bands[i].data[indexInput];
            }
            if (isEven) {
               output.data[indexOutput] =  (QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length) + 
                       QuickSelect.select(valueArray, valueArray.length / 2 + 1, valueArray.length)) / 2;
            } else {
               output.data[indexOutput] = QuickSelect.select(valueArray, valueArray.length / 2, valueArray.length);
            }
         }
		}
	}
   
   
   
}

