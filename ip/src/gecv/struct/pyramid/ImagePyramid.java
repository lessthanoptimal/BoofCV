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
 * <p>Image pyramids represent the same image at multiple resolutions allowing scale space searches to performed.</p>
 *
 * <p>
 * The scaling is relative to the previous layer.  For example, scale = [1,2,2] would be three layers which have scaling of 1,2, and 4 relative to the original image.
 * The dimension of each image is the dimension of the previous layer dividing by its scaling.  So if the upper
 * layer has a width/height of (640,480) and the next layer has a scale factor of 2, its dimension will be (320,240).
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
	 * Updates each level in the pyramid using the specified input image.
	 * 
	 * @param image Original input image.
	 */
	public void update( T image ) {
		if( updater == null ) {
			throw new IllegalArgumentException("Updater is null, this is an error or the class should be updated manually.");
		}

		if( layers == null ) {
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
	 * Returns the scale factor relative to the original image.
	 *
	 * @param layer Layer at which the scale factor is to be computed.
	 * @return Scale factor relative to original image.
	 */
	public double getScalingAtLayer(int layer) {
		double scale = 1;

		for (int i = 0; i <= layer; i++) {
			scale *= getScale(i);
		}

		return scale;
	}

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
		return layers[layer].width;
	}

	public int getHeight(int layer) {
		return layers[layer].height;
	}
}
