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

import java.lang.reflect.Array;

/**
 * <p>Image pyramids represent the same image at multiple resolutions allowing scale space searches to performed.</p>
 * 
 * <p>
 * When updating the pyramid, if the top most layer is at the same resolution as the original image then a reference
 * can optionally be saved, avoiding an unnecissary image copy.  This is done by setting the saveOriginalReference
 * to true.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class ImagePyramid<T extends ImageBase> {
	// shape of full resolution input image
	public int bottomWidth;
	public int bottomHeight;

	// The image at different resolutions.  Larger indexes for lower resolutions
	public T layers[];
	// scale of each layer relative to the previous layer
	public int scale[];
	// if the top layer is full resolution, should a copy be made or a reference to the original be saved?i
	public boolean saveOriginalReference;

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param bottomWidth		   Width of original full resolution image.
	 * @param bottomHeight		  Height of original full resolution image.
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of  copied.
	 */
	public ImagePyramid(int bottomWidth, int bottomHeight, boolean saveOriginalReference) {
		this.saveOriginalReference = saveOriginalReference;
		this.bottomWidth = bottomWidth;
		this.bottomHeight = bottomHeight;
	}

	/**
	 * <p>Sets the scale factor for each layer in the pyramid.</p>
     *
	 * <p>
	 * The scaling is relative to the previous layer.  For
	 * example, scale = [1,2,2] would be three layers which have scaling of 1,2, and 4 relative to the original image.
	 * The dimension of each image is the dimension of the previous layer dividing by its scaling.  So if the upper
	 * layer has a width/height of (640,480) and the next layer has a scale factor of 2, its dimension will be (320,240).
	 * </p>
	 *
	 * @param scale
	 */
	public void setScaling(int... scale) {
		if (scale.length <= 0)
			throw new IllegalArgumentException("A scale must be specified");
		for (int s : scale)
			if (s < 1)
				throw new IllegalArgumentException("The scale of each layer must be >= 1");

		Class<T> type = getImageType();

		layers = (T[]) Array.newInstance(type, scale.length);
		this.scale = new int[scale.length];
		System.arraycopy(scale, 0, this.scale, 0, scale.length);
		int scaleFactor = scale[0];

		if (scale[0] == 1) {
			if (!saveOriginalReference) {
				layers[0] = createImage(bottomWidth, bottomHeight);
			}
		} else {
			layers[0] = createImage(bottomWidth / scaleFactor, bottomHeight / scaleFactor);
		}

		for (int i = 1; i < scale.length; i++) {
			scaleFactor *= scale[i];
			layers[i] = createImage(bottomWidth / scaleFactor, bottomHeight / scaleFactor);
		}
	}

	/**
	 * Returns the scale factor relative to the original image.
	 *
	 * @param layer Layer at which the scale factor is to be computed.
	 * @return Scale factor relative to original image.
	 */
	public int getScalingAtLayer(int layer) {
		int scale = 1;

		for (int i = 0; i <= layer; i++) {
			scale *= this.scale[i];
		}

		return scale;
	}

	/**
	 * Internally used to create a new layer in an abstract way.
	 *
	 * @param width  Image's width.
	 * @param height Image's height.
	 * @return The new image.
	 */
	protected abstract T createImage(int width, int height);

	/**
	 * Type of image in each layer of the pyramid.
	 *
	 * @return Image type.
	 */
	public abstract Class<T> getImageType();

	/**
	 * Returns a layer in the pyramid.
	 *
	 * @param layerNum which image is to be returned.
	 * @return The image in the pyramid.
	 */
	public T getLayer(int layerNum) {
		return layers[layerNum];
	}

	public int getNumLayers() {
		return layers.length;
	}

	public int getWidth(int layer) {
		return bottomWidth / getScalingAtLayer(layer);
	}

	public int getHeight(int layer) {
		return bottomHeight / getScalingAtLayer(layer);
	}
}
