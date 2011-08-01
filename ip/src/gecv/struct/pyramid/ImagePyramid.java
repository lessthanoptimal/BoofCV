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

import gecv.core.image.ImageGenerator;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.image.ImageBase;

/**
 * <p>
 * Image pyramids represent the same image at multiple resolutions allowing searches to performed across multiple
 * resolutions.  There are many different ways to defined an image pyramid and different ways to compute it.  This
 * is a base class which hides many of those issues.
 * </p>
 *
 * <p>
 * When updating the pyramid, if the top most layer is at the same resolution as the original image then a reference
 * can optionally be saved, avoiding an unnecessary image copy.  This is done by setting the saveOriginalReference
 * to true.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class ImagePyramid<T extends ImageBase> {

	// class which updates the pyramid
	public PyramidUpdater<T> updater;

	// shape of full resolution input image
	public int bottomWidth;
	public int bottomHeight;

	// The image at different resolutions.  Larger indexes for lower resolutions
	public T layers[];

	// if the top layer is full resolution, should a copy be made or a reference to the original be saved?i
	public boolean saveOriginalReference;

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of  copied.
	 * @param updater Specifies how the image pyramid is updated.
	 */
	public ImagePyramid(boolean saveOriginalReference,
						PyramidUpdater<T> updater ) {
		this.saveOriginalReference = saveOriginalReference;
		this.updater = updater;
	}
	/**
	 * Used to internally check that the provided scales are valid.
	 */
	protected void checkScales() {
		if( getScale(0) < 0 ) {
			throw new IllegalArgumentException("The first layer must be more than zero.");
		}

		double prevScale = 0;
		for( int i = 0; i < getNumLayers(); i++ ) {
			double s = getScale(i);
			if( s <= prevScale )
				throw new IllegalArgumentException("Higher layers must have larger scale factors than previous layers.");
			prevScale = s;
		}
	}

	/**
	 * Updates each level in the pyramid using the specified input image.
	 * 
	 * @param image Original input image.
	 */
	public void update( T image ) {
		if( updater == null ) {
			throw new IllegalArgumentException("Updater is null, this is an error or the class should be updated manually.");
		}

		if( !isLayersDeclared() ) {
			ImageGenerator<T> gen = (ImageGenerator<T>)FactoryImageGenerator.create(image.getClass());
			declareLayers(gen, image.width,image.height);
		}

		updater.update(image,this);
	}

	/**
	 * Declares the layers in the pyramid.
	 *
	 * @param generator
	 */
	public abstract void declareLayers( ImageGenerator<T> generator , int width , int height );

	/**
	 *
	 * @param layer
	 * @return
	 */
	public abstract double getScale( int layer );

	/**
	 * Returns a layer in the pyramid.
	 *
	 * @param layerNum which image is to be returned.
	 * @return The image in the pyramid.
	 */
	public T getLayer(int layerNum) {
		return layers[layerNum];
	}

	public abstract int getNumLayers();

	public int getWidth(int layer) {
		return layers[layer].width;
	}

	public int getHeight(int layer) {
		return layers[layer].height;
	}

	public boolean isLayersDeclared() {
		return layers != null;
	}
}
