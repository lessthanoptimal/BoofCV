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

package boofcv.struct.pyramid;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * <p>
 * Base class for image pyramids. Provides common functionality and data structures. The scale
 * is defined in its children {@link PyramidDiscrete} and {@link PyramidFloat}. This allows
 * it to be either integer or floating point and strongly typed.
 * </p>
 *
 * <p>
 * When updating the pyramid, if the top most layer is at the same resolution as the original image then a reference
 * can optionally be saved, avoiding an unnecessary image copy. This is done by setting the saveOriginalReference
 * to true.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ImagePyramidBase<T extends ImageBase<T>>
		implements ImagePyramid<T> {
	// shape of full resolution input image
	protected int bottomWidth;
	protected int bottomHeight;

	// The image at different resolutions. Larger indexes for lower resolutions
	public T[] layers;

	// if the top layer is full resolution, should a copy be made or a reference to the original be saved?i
	protected boolean saveOriginalReference;

	ImageType<T> imageType;

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param imageType Type of image which is processed
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of copied.
	 */
	protected ImagePyramidBase( ImageType<T> imageType, boolean saveOriginalReference ) {
		this.imageType = imageType;
		this.saveOriginalReference = saveOriginalReference;
	}

	protected ImagePyramidBase( ImagePyramidBase<T> orig ) {
		this.imageType = orig.imageType;
		this.saveOriginalReference = orig.saveOriginalReference;

		if (orig.layers != null) {
			layers = imageType.createArray(orig.layers.length);
			for (int i = 0; i < layers.length; i++) {
				if (orig.layers[i] != null) {
					this.layers[i] = orig.layers[i].createSameShape();
				}
			}
		}
	}

	/**
	 * Initializes internal data structures based on the input image's size. Should be called each time a new image
	 * is processed.
	 *
	 * @param width Image width
	 * @param height Image height
	 */
	@Override
	public void initialize( int width, int height ) {
		// see if it has already been initialized
		if (bottomWidth == width && bottomHeight == height)
			return;

		this.bottomWidth = width;
		this.bottomHeight = height;
		layers = imageType.createArray(getNumLayers());
		double scaleFactor = getScale(0);

		if (scaleFactor == 1) {
			if (!saveOriginalReference) {
				layers[0] = imageType.createImage(bottomWidth, bottomHeight);
			}
		} else {
			layers[0] = imageType.createImage((int)Math.ceil(bottomWidth/scaleFactor), (int)Math.ceil(bottomHeight/scaleFactor));
		}

		for (int i = 1; i < layers.length; i++) {
			scaleFactor = getScale(i);
			layers[i] = imageType.createImage((int)Math.ceil(bottomWidth/scaleFactor), (int)Math.ceil(bottomHeight/scaleFactor));
		}
	}

	@Override
	public boolean isInitialized() {
		return layers != null;
	}

	/**
	 * Used to internally check that the provided scales are valid.
	 */
	protected void checkScales() {
		if (getScale(0) < 0) {
			throw new IllegalArgumentException("The first layer must be more than zero.");
		}

		double prevScale = 0;
		for (int i = 0; i < getNumLayers(); i++) {
			double s = getScale(i);
			if (s < prevScale)
				throw new IllegalArgumentException("Higher layers must be the same size or larger than previous layers.");
			prevScale = s;
		}
	}

	@Override
	public void setTo( ImagePyramid<T> input ) {
		for (int i = 0; i < layers.length; i++) {
			getLayer(i).setTo(input.getLayer(i));
		}
	}

	@Override
	public T getLayer( int layerNum ) {
		return layers[layerNum];
	}

	public void setFirstLayer( T image ) {
		if (saveOriginalReference) {
			layers[0] = image;
		} else {
			throw new IllegalArgumentException("Attempting to set the first layer when saveOriginalReference is false");
		}
	}

	@Override
	public int getWidth( int layer ) {
		return layers[layer].width;
	}

	@Override
	public int getHeight( int layer ) {
		return layers[layer].height;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public int getInputWidth() {
		return bottomWidth;
	}

	@Override
	public int getInputHeight() {
		return bottomHeight;
	}

	public boolean isSaveOriginalReference() {
		return saveOriginalReference;
	}

	public void setSaveOriginalReference( boolean save ) {
		this.saveOriginalReference = save;
	}
}
