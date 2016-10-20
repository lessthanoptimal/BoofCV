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

package boofcv.struct.image;

import boofcv.core.image.GeneralizedImageOps;

import java.lang.reflect.Array;

/**
 * <p>
 * Multi-band image composed of discontinuous planar images for each band.  The bands are discontinuous in that
 * each one is an independent memory and are of type {@link ImageGray}.  Planar images fully supports all
 * functions inside of {@link ImageBase}. Each internal image has the same width, height, startIndex, and stride.
 * </p>
 * 
 * <p>
 * For example, in a RGB image there would be three bands one for each color,
 * and each color would be stored in its own gray scale image.  To access the image for a particular
 * band call {@link #getBand(int)}. To get the RGB value for a pixel (x,y) one would need to:
 * <pre>
 * int red = image.get(0).get(x,y);
 * int green = image.get(1).get(x,y);
 * int blue = image.get(2).get(x,y);
 * </pre>
 * Setting the RGB value of pixel (x,y) is done in a similar manor:
 * <pre>
 * image.get(0).get(x,y,red);
 * image.get(1).get(x,y,green);
 * image.get(2).get(x,y,blue);
 * </pre>
 * </p>
 * 
 * <p>
 * May image processing operations can be run independently on each color band. This is useful since many
 * operations have been written for {@link ImageGray}, but not Planar yet.
 * <pre>
 * for( int i = 0; i < image.numBands(); i++ ) {
 *     SomeGrayImageFilter.process( image.getBand(0) );
 * }    
 * </pre>
 * </p>
 *
 * @author Peter Abeles
 */
public class Planar<T extends ImageGray> extends ImageMultiBand<Planar<T>>{

	/**
	 * Type of image in each band
	 */
	public Class<T> type;

	/**
	 * Set of gray scale images
	 */
	public T bands[];

	/**
	 * Creates a Planar image with the specified properties.
	 *
	 * @param type The type of image which each band is stored as.
	 * @param width Width of the image.
	 * @param height Height of the image.
	 * @param numBands Total number of bands.
	 */
	public Planar(Class<T> type, int width, int height, int numBands) {
		this.type = type;
		this.stride = width;
		this.width = width;
		this.height = height;
		this.bands = (T[]) Array.newInstance(type, numBands);

		for (int i = 0; i < numBands; i++) {
			bands[i] = GeneralizedImageOps.createSingleBand(type, width, height);
		}
		this.imageType = ImageType.pl(numBands,type);
	}
	
	/**
	 * Declares internal arrays for storing each band, but not the images in each band.
	 *
	 * @param type The type of image which each band is stored as. 
	 * @param numBands Number of bands in the image.
	 */
	public Planar(Class<T> type, int numBands) {
		this.type = type;
		this.bands = (T[]) Array.newInstance(type, numBands);
		this.imageType = ImageType.pl(numBands,type);
	}


	/**
	 * Type of image each band is stored as.
	 * 
	 * @return The type of ImageGray which each band is stored as.
	 */
	public Class<T> getBandType() {
		return type;
	}

	/**
	 * Returns the number of bands or colors stored in this image.
	 * 
	 * @return Number of bands in the image.
	 */
	@Override
	public int getNumBands() {
		return bands.length;
	}

	/**
	 * Returns a band in the multi-band image.
	 * 
	 * @param band Which band should be returned.
	 * @return Image band
	 */
	public T getBand(int band) {
		if (band >= bands.length || band < 0)
			throw new IllegalArgumentException("The specified band is out of bounds: "+band);

		return bands[band];
	}

	/**
	 * Creates a sub-image from 'this' image.  The subimage will share the same internal array
	 * that stores each pixel's value, but will only pertain to an axis-aligned rectangular segment
	 * of the original.
	 *
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image.
	 * @param y0 y-coordinate of top-left corner of the sub-image.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image.
	 * @param subimage
	 * @return A sub-image of this image.
	 */
	@Override
	public Planar<T> subimage(int x0, int y0, int x1, int y1, Planar<T> subimage) {
		if (x0 < 0 || y0 < 0)
			throw new IllegalArgumentException("x0 or y0 is less than zero");
		if (x1 < x0 || y1 < y0)
			throw new IllegalArgumentException("x1 or y1 is less than x0 or y0 respectively");
		if (x1 > width || y1 > height)
			throw new IllegalArgumentException("x1 or y1 is more than the width or height respectively");

		Planar<T> ret = new Planar<>(type, bands.length);
		ret.stride = Math.max(width, stride);
		ret.width = x1 - x0;
		ret.height = y1 - y0;
		ret.startIndex = startIndex + y0 * stride + x0;
		ret.subImage = true;

		for( int i = 0; i < bands.length; i++ ) {
			ret.bands[i] = (T)bands[i].subimage(x0,y0,x1,y1);
		}
		
		return ret;
	}

	/**
	 * Sets the values of each pixel equal to the pixels in the specified matrix.
	 * Automatically resized to match the input image.
	 *
	 * @param orig The original image whose value is to be copied into this one
	 */
	@Override
	public void setTo( Planar<T> orig) {
		if (orig.width != width || orig.height != height)
			reshape(orig.width,orig.height);
		if( orig.getNumBands() != getNumBands() )
			throw new IllegalArgumentException("The number of bands must be the same");
		if( orig.getBandType() != getBandType() )
			throw new IllegalArgumentException("The band type must be the same");

		int N = orig.getNumBands();
		for( int i = 0; i < N; i++ ) {
			bands[i].setTo(orig.getBand(i));
		}
	}

	/**
	 * Changes the image's width and height without declaring new memory.  If the internal array
	 * is not large enough to store the new image an IllegalArgumentException is thrown.
	 *
	 * @param width The new width.
	 * @param height The new height.
	 */
	@Override
	public void reshape(int width, int height) {

		for( int i = 0; i < bands.length; i++ ) {
			bands[i].reshape(width,height);
		}

		this.startIndex = 0;
		this.stride = width;
		this.width = width;
		this.height = height;
	}

	/**
	 * Creates a new image of the same type and number of bands
	 *
	 * @param imgWidth image width
	 * @param imgHeight image height
	 * @return new image
	 */
	@Override
	public Planar<T> createNew(int imgWidth, int imgHeight) {
		return new Planar<>(type, imgWidth, imgHeight, bands.length);
	}

	/**
	 * Returns a new {@link Planar} which references the same internal single band images at this one.
	 *
	 * @param which List of bands which will comprise the new image
	 * @return New image
	 */
	public Planar<T> partialSpectrum(int ...which ) {
		Planar<T> out = new Planar<>(getBandType(), which.length);

		out.setWidth(width);
		out.setHeight(height);
		out.setStride(stride);

		for (int i = 0; i < which.length; i++) {
			out.setBand(i,getBand(which[i]));
		}

		return out;
	}

	/**
	 * Changes the bands order
	 * @param order The new band order
	 */
	public void reorderBands( int ...order ) {
		T[] bands = (T[]) Array.newInstance(type, order.length);

		for (int i = 0; i < order.length; i++) {
			bands[i] = this.bands[order[i]];
		}
		this.bands = bands;
	}

	public void setBandType(Class<T> type) {
		this.type = type;
	}

	public T[] getBands() {
		return bands;
	}

	public void setBands(T[] bands) {
		this.bands = bands;
	}

	public void setBand( int which , T image ) {
		this.bands[which] = image;
	}
}
