/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct.pyramid;

import gecv.struct.image.ImageBase;

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
 * Usage: Before being used the scales in the pyramid must first be specified then the {@link @initialize}
 * function called.  How the scale is set is implementation dependent.  Once initialized images
 * at different layers can be accessed with {@link #getLayer}.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public interface ImagePyramid<T extends ImageBase> {

	/**
	 * Creates the pyramids internal data structures.  The provided image must be of the same type
	 * and dimension as all the input images.
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
	 * Checks to see if the image pyramid has been initialized or not yet.
	 * @return True if initialized and false if not.
	 */
	public boolean isInitialized();

	/**
	 * The type of image.
	 *
	 * @return Image type.
	 */
	public Class<T> getImageType();
}
