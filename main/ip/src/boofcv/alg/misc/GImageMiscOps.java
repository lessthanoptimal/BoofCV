/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import java.util.Random;

/**
 * Generalized version of {@link ImageMiscOps}.  Type checking is performed at runtime instead of at compile type.
 *
 * @author Peter Abeles
 */
public class GImageMiscOps {

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX , int srcY , int dstX , int dstY , int width , int height ,
							 ImageBase input , ImageBase output ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageInt8) input, (ImageInt8) output);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageInt16) input, (ImageInt16) output);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageSInt32) input, (ImageSInt32) output);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageSInt64) input, (ImageSInt64) output);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageFloat32) input, (ImageFloat32) output);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, (ImageFloat64) input, (ImageFloat64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral mi = (MultiSpectral)input;
			MultiSpectral mo = (MultiSpectral)output;
			for( int i = 0; i < mi.getNumBands(); i++ )
				copy(srcX,srcY,dstX,dstY,width,height,mi.getBand(i),mo.getBand(i));
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

		/**
		 * Computes the mean of the absolute value of the difference between the two images.
		 *
		 * @param input Input image. Not modified.
		 * @param value fill value
		 */
	public static void fill( ImageBase input , double value ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fill((ImageInt8)input,(int)value);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fill((ImageInt16)input,(int)value);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.fill((ImageSInt32)input,(int)value);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.fill((ImageSInt64)input,(long)value);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.fill((ImageFloat32)input,(float)value);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.fill((ImageFloat64)input,value);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if( InterleavedI8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fill((InterleavedI8)input,(int)value);
			} else if( InterleavedI16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fill((InterleavedI16)input,(int)value);
			} else if( InterleavedS32.class == input.getClass() ) {
				ImageMiscOps.fill((InterleavedS32)input,(int)value);
			} else if( InterleavedS64.class == input.getClass() ) {
				ImageMiscOps.fill((InterleavedS64)input,(long)value);
			} else if( InterleavedF32.class == input.getClass() ) {
				ImageMiscOps.fill((InterleavedF32)input,(float)value);
			} else if( InterleavedF64.class == input.getClass() ) {
				ImageMiscOps.fill((InterleavedF64)input,value);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				fill(m.getBand(i),value);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param input An image.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( ImageBase input , double value , int radius ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillBorder((ImageInt8) input, (int) value, radius);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillBorder((ImageInt16) input, (int) value, radius);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.fillBorder((ImageSInt32) input, (int) value, radius);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.fillBorder((ImageSInt64) input, (long) value, radius);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.fillBorder((ImageFloat32) input, (float) value, radius);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.fillBorder((ImageFloat64) input, value, radius);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				fillBorder(m.getBand(i), value, radius);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param input Image the rectangle is drawn in.  Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( ImageBase input , double value, int x0, int y0, int width, int height ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillRectangle((ImageInt8)input,(int)value,x0,y0,width,height);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillRectangle((ImageInt16)input,(int)value,x0,y0,width,height);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.fillRectangle((ImageSInt32)input,(int)value,x0,y0,width,height);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.fillRectangle((ImageSInt64)input,(long)value,x0,y0,width,height);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.fillRectangle((ImageFloat32)input,(float)value,x0,y0,width,height);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.fillRectangle((ImageFloat64)input,value,x0,y0,width,height);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				fillRectangle(m.getBand(i),value,x0,y0,width,height);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution.  A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range.  A drawn value outside the allowed range will be set to the closest bound.
	 *
	 * @param input Input image.  Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( ImageBase input , Random rand , double mean , double sigma , double lowerBound , double upperBound ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillGaussian((ImageInt8) input, rand, mean, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillGaussian((ImageInt16) input, rand, mean, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.fillGaussian((ImageSInt32) input, rand, mean, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.fillGaussian((ImageSInt64) input, rand, mean, sigma, (long) lowerBound, (long) upperBound);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.fillGaussian((ImageFloat32) input, rand, mean, sigma, (float) lowerBound, (float) upperBound);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.fillGaussian((ImageFloat64) input, rand, mean, sigma, lowerBound, upperBound);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min <= X < max.
	 *
	 * @param input Image which is to be filled.  Modified,
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution.  Inclusive.
	 * @param max Maximum value of the distribution.  Inclusive.
	 */
	public static void fillUniform( ImageBase input , Random rand , double min , double max  ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillUniform((ImageInt8) input, rand, (int) min, ((int)max)-1);
			} else if( ImageInt16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillUniform((ImageInt16) input, rand, (int) min, ((int)max)-1);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.fillUniform((ImageSInt32) input, rand, (int) min, ((int)max)-1);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.fillUniform((ImageSInt64) input, rand, (long) min, ((long)max)-1);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.fillUniform((ImageFloat32) input, rand, (float)min, (float) max);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.fillUniform((ImageFloat64) input, rand, min,  max);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if( InterleavedI8.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillUniform((InterleavedI8)input,rand, (int) min, ((int)max)-1);
			} else if( InterleavedI16.class.isAssignableFrom(input.getClass()) ) {
				ImageMiscOps.fillUniform((InterleavedI16)input,rand, (int) min, ((int)max)-1);
			} else if( InterleavedS32.class == input.getClass() ) {
				ImageMiscOps.fillUniform((InterleavedS32)input,rand, (int) min, ((int)max)-1);
			} else if( InterleavedS64.class == input.getClass() ) {
				ImageMiscOps.fillUniform((InterleavedS64)input,rand, (long) min, ((long)max)-1);
			} else if( InterleavedF32.class == input.getClass() ) {
				ImageMiscOps.fillUniform((InterleavedF32)input,rand, (float)min, (float) max);
			} else if( InterleavedF64.class == input.getClass() ) {
				ImageMiscOps.fillUniform((InterleavedF64)input,rand, min,  max);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				fillUniform(m.getBand(i), rand , min, max);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image.  If a value exceeds the specified
	 * it will be set to the closest bound.
	 *
	 * @param input Input image.  Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian( ImageBase input, Random rand , double sigma ,
									double lowerBound , double upperBound  )
	{
		if( input instanceof ImageSingleBand ) {
			if( ImageUInt8.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageUInt8) input, rand, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt8.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageSInt8) input, rand, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageUInt16.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageUInt16) input, rand, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt16.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageSInt16) input, rand, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageSInt32) input, rand, sigma, (int) lowerBound, (int) upperBound);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageSInt64) input, rand, sigma, (long) lowerBound, (long) upperBound);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageFloat32) input, rand, sigma, (float) lowerBound, (float) upperBound);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.addGaussian((ImageFloat64) input, rand, sigma, lowerBound, upperBound);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				addGaussian(m.getBand(i), rand, sigma, lowerBound, upperBound);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image.  Noise range is min <= X < max.
	 */
	public static void addUniform( ImageBase input, Random rand , double min , double max  ) {
		if( input instanceof ImageSingleBand ) {
			if( ImageUInt8.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageUInt8) input, rand, (int) min, (int) max);
			} else if( ImageSInt8.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageSInt8) input, rand, (int) min, (int) max);
			} else if( ImageUInt16.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageUInt16) input, rand, (int) min, (int) max);
			} else if( ImageSInt16.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageSInt16) input, rand, (int) min, (int) max);
			} else if( ImageSInt32.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageSInt32) input, rand, (int) min, (int) max);
			} else if( ImageSInt64.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageSInt64) input, rand, (long) min, (long) max);
			} else if( ImageFloat32.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageFloat32) input, rand, (float) min, (float) max);
			} else if( ImageFloat64.class == input.getClass() ) {
				ImageMiscOps.addUniform((ImageFloat64) input, rand, min, max);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
			}
		} else if( input instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)input;
			for( int i = 0; i < m.getNumBands(); i++ )
				addUniform(m.getBand(i), rand, min, max);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Flips the image from top to bottom
	 */
	public static void flipVertical( ImageBase img ) {
		if( img instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageInt8)img);
			} else if( ImageInt16.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageInt16)img);
			} else if ( ImageSInt32.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageSInt32)img);
			} else if ( ImageSInt64.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageSInt64)img);
			} else if (ImageFloat32.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageFloat32)img);
			} else if (ImageFloat64.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipVertical((ImageFloat64)img);
			} else {
				throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
			}
		} else if( img instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)img;
			for( int i = 0; i < m.getNumBands(); i++ )
				flipVertical(m.getBand(i));
		} else {
			throw new IllegalArgumentException("Unknown image type: " + img.getClass().getSimpleName());
		}
	}

	/**
	 * Flips the image from left to right
	 */
	public static void flipHorizontal( ImageBase img ) {
		if( img instanceof ImageSingleBand ) {
			if( ImageInt8.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageInt8) img);
			} else if( ImageInt16.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageInt16) img);
			} else if ( ImageSInt32.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageSInt32) img);
			} else if ( ImageSInt64.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageSInt64) img);
			} else if (ImageFloat32.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageFloat32) img);
			} else if (ImageFloat64.class.isAssignableFrom(img.getClass()) ) {
				ImageMiscOps.flipHorizontal((ImageFloat64) img);
			} else {
				throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
			}
		} else if( img instanceof MultiSpectral ) {
			MultiSpectral m = (MultiSpectral)img;
			for( int i = 0; i < m.getNumBands(); i++ )
				flipHorizontal(m.getBand(i));
		} else {
			throw new IllegalArgumentException("Unknown image type: " + img.getClass().getSimpleName());
		}
	}
}
