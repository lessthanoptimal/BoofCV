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

package boofcv.struct.pyramid;

import boofcv.struct.image.ImageGray;


/**
 * <p>
 * In this implementation the scale factor between each layer is limited to being a positive integer that is evenly
 * divisible by the previous layer.  This added constraint allows further optimization to be performed.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class PyramidDiscrete<T extends ImageGray> extends ImagePyramidBase<T> {

	// scale of each layer relative to the previous layer
	public int scale[];

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param imageType Type of image.
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of copied.
	 *                              Set to false if you don't know what you are doing.
	 * @param scaleFactors (optional) Specifies the scale of each layer in the pyramid.  See restrictions
	 * on scaleFactor in {@link #setScaleFactors(int...)}.
	 */
	public PyramidDiscrete( Class<T> imageType ,
							boolean saveOriginalReference, int ...scaleFactors)
	{
		super(imageType,saveOriginalReference);
		if( scaleFactors.length > 0 )
			setScaleFactors(scaleFactors);
	}

	/**
	 * Specifies the pyramid's structure.  Scale factors are in relative to the input image.
	 *
	 * @param scaleFactors Change in scale factor for each layer in the pyramid.
	 */
	public void setScaleFactors( int ...scaleFactors ) {
		for( int i = 1; i < scaleFactors.length; i++ ){
			if( scaleFactors[i] % scaleFactors[i-1] != 0 ) {
				throw new IllegalArgumentException("Layer "+i+" is not evenly divisible by its lower layer.");
			}
		}

		// set width/height to zero to force the image to be redeclared
		bottomWidth = bottomHeight = 0;
		this.scale = scaleFactors.clone();
		checkScales();
	}

	public int[] getScales() {
		return scale;
	}

	@Override
	public double getScale(int layer) {
		return scale[layer];
	}

	@Override
	public int getNumLayers() {
		return scale.length;
	}
}
