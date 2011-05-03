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

package gecv.alg.pyramid;

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
	protected int topWidth;
	protected int topHeight;

	// The image at different resolutions.  Larger indexes for lower resolutions
	protected T layers[];
	// scale of each layer relative to the previous layer
	protected int scale[];
	// if the top layer is full resolution, should a copy be made or a reference to the original be saved?i
	boolean saveOriginalReference;

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param topWidth Width of original full resolution image.
	 * @param topHeight Height of original full resolution image.
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of  copied.
	 */
	public ImagePyramid( int topWidth, int topHeight , boolean saveOriginalReference )
	{
		this.saveOriginalReference = saveOriginalReference;
		this.topWidth = topWidth;
		this.topHeight = topHeight;
	}

	/**
	 *
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
	public void setScaling( int ...scale ) {
		if( scale.length <= 0 )
			throw new IllegalArgumentException("A scale must be specified");
		for( int s : scale )
			if( s < 1 )
				throw new IllegalArgumentException("The scale of each layer must be >= 1");

		Class<T> type = getImageType();

		layers = (T[])Array.newInstance( type , scale.length );
		this.scale = new int[ scale.length ];
		System.arraycopy(scale,0,this.scale,0,scale.length);
		int scaleFactor = scale[0];

		if( scale[0] == 1 ) {
			if( !saveOriginalReference ) {
				layers[0] = createImage(topWidth,topHeight);
			}
		} else {
			layers[0] = createImage(topWidth/scaleFactor,topHeight/scaleFactor);
		}

		for( int i = 1; i < scale.length; i++ ){
			scaleFactor *= scale[i];
			layers[i] = createImage(topWidth/scaleFactor,topHeight/scaleFactor);
		}
	}

	/**
	 * Internally used to create a new layer in an abstract way.
	 *
	 * @param width Image's width.
	 * @param height Image's height.
	 * @return The new image.
	 */
	protected abstract T createImage( int width , int height );

	/**
	 * Type of image in each layer of the pyramid.
	 * 
	 * @return Image type.
	 */
	public abstract Class<T> getImageType();

	/**
	 * Given this original image create a pyramidal scale space representation.
	 *
	 * @param original Original full resolution image
	 */
	public void update( T original ) {
		if( original.width != topWidth || original.height != topHeight )
			throw new IllegalArgumentException("Unexpected dimension");
		_update(original);
	}

	/**
	 * Returns a layer in the pyramid.
	 *
	 * @param layerNum which image is to be returned.
	 * @return The image in the pyramid.
	 */
	public T getLayer( int layerNum ) {
		return layers[layerNum];
	}

	/**
	 * Internal implementation specific function which updates the pyramid.
	 * @param original Original full resolution image
	 */
	public abstract void _update( T original );

}
