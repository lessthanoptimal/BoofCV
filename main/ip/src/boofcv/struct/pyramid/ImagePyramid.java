/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.pyramid;

import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Image pyramids represent an image at different resolution in a fine to coarse fashion. Lower layers
 * in the pyramid are at a higher resolution than the upper layers.  The resolution of a layer is
 * specified by its scale.  The scale number indicates how many pixels in the original input image
 * correspond to a single pixel at the current layer.  So a layer with a scale of 5 is 5 times lower
 * resolution than the input layer.
 * </p>
 *
 * <p>
 * The transform from a pixel coordinate in layer 'i' to the original image will vary depending on the pyramid is
 * constructed.  In general it, can be described by the following equation: (x,y) = (offX_i,offY_i) + scale_i*(x_i,y_i),
 * where (x_i,y_i) is the pixel coordinate in layer 'i'. The offsets (offX_i,offY_i) vary depending on how
 * each layer in the pyramid samples the previous layers.  This offset can be found by calling {@link #getSampleOffset(int)}.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public interface ImagePyramid<T extends ImageBase> {

	/**
	 * Constructs the image pyramid given the input image.
	 */
	public void process( T input );

	/**
	 * Declares internal data structures for an image with the specified dimensions
	 * @param width image width
	 * @param height image height
	 */
	public void initialize( int width , int height );

	/**
	 * Returns the scale of the specified layer in the pyramid.  Larger the scale
	 * smaller the image is relative to the input image.
	 *
	 * @param layer Which layer is being inspected.
	 * @return The layer's scale.
	 */
	public double getScale( int layer );

	/**
	 * Returns a layer in the pyramid.
	 *
	 * @param layerNum which image is to be returned.
	 * @return The image in the pyramid.
	 */
	public T getLayer(int layerNum);

	/**
	 * Returns the number of layers in the pyramid.
	 */
	public int getNumLayers();

	/**
	 * Returns the width of an image at ths specified layer.
	 *
	 * @param layer The layer being requested.
	 * @return The layer's width.
	 */
	public int getWidth(int layer);

	/**
	 * Returns the height of an image at ths specified layer.
	 *
	 * @param layer The layer being requested.
	 * @return The layer's height.
	 */
	public int getHeight(int layer);

	/**
	 * Width of input image.
	 */
	public int getInputWidth();

	/**
	 * Height of input image.
	 */
	public int getInputHeight();

	/**
	 * The type of image.
	 *
	 * @return Image type.
	 */
	public Class<T> getImageType();

	/**
	 * Set's this pyramid to be the same as input.  The two pyramids must have the same structure or else an
	 * exception will be thrown.
	 * @param input Input pyramid.  Not modified.
	 */
	public void setTo( ImagePyramid<T> input );

	/**
	 * Returns the sampling offset.  Both x and y axises are assumed to have the same offset.
	 * See comment in constructor above.
	 *
	 * @param layer Layer in the pyramid
	 * @return Sampling offset in pixels.
	 */
	public double getSampleOffset( int layer );

	/**
	 * Returns the scale-space scale for the specified layer.  This scale is equivalent amount of Gaussian blur
	 * applied to the input image.
	 *
	 * If Gaussian blur is not applied to each layer then an approximation should be returned.
	 *
	 * @param layer Layer in the pyramid
	 * @return Equivalent sigma for Gaussian blur.
	 */
	public double getSigma( int layer );
}
