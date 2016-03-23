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
 * An image pyramid where each level can be an arbitrary scale.  Higher levels in the pyramid
 * are sub-sampled from lower levels in the pyramid allowing fractions of a pixel change.  This
 * added flexibility comes at the cost of some performance relative to {@link PyramidDiscrete}.
 * </p>
 * 
 * <p>
 * An {@link ImagePyramid} where the scale factor between each level is specified using a floating point number.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class PyramidFloat<T extends ImageGray> extends ImagePyramidBase<T> {

	// scale of each layer relative to the previous layer
	public double scale[];

	/**
	 * Defines the image pyramid.
	 *
	 * @param imageType Type of image
	 * @param scaleFactors (optional) Specifies the scale of each layer in the pyramid.  See restrictions
	 * on scaleFactor in {@link #setScaleFactors(double...)}.
	 */
	public PyramidFloat( Class<T> imageType , double ...scaleFactors ) {
		super(imageType,false);
		if( scaleFactors.length > 0 )
			setScaleFactors(scaleFactors);
	}

	/**
	 * Specifies the pyramid's structure.
	 *
	 * @param scaleFactors Change in scale factor for each layer in the pyramid.
	 */
	public void setScaleFactors( double ...scaleFactors ) {
		// see if the scale factors have not changed
		if( scale != null && scale.length == scaleFactors.length ) {
			boolean theSame = true;
			for( int i = 0; i < scale.length; i++ ) {
				if( scale[i] != scaleFactors[i] ) {
					theSame = false;
					break;
				}
			}
			// no changes needed
			if( theSame )
				return;

		}
		// set width/height to zero to force the image to be redeclared
		bottomWidth = bottomHeight = 0;
		this.scale = scaleFactors.clone();
		checkScales();
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
