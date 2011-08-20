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
import gecv.struct.image.ImageBase;

import java.lang.reflect.Array;


/**
 * <p>
 * An image pyramid where each level can be an arbitrary scale.  Higher levels in the pyramid
 * are subsampled from lower levels in the pyramid allowing fractions of a pixel change.  This
 * added flexibility comes at the cost of some performance relative to {@link gecv.struct.pyramid.DiscreteImagePyramid}.
 * </p>
 * 
 * <p>
 * An {@link ImagePyramid} where the scale factor between each level is specified using a floating point number.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class SubsamplePyramid<T extends ImageBase> extends ImagePyramid<T> {

	// scale of each layer relative to the previous layer
	public double scale[];

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param updater How each layer in the pyramid is computed.
	 * @param scaleFactors The scales in the pyramid.
	 */
	public SubsamplePyramid( PyramidUpdater<T> updater , double ...scaleFactors ) {
		super(false,updater);
		setScaleFactors(scaleFactors);
	}

	/**
	 * Creates a pyramid which cannot be updated using {@link #update(gecv.struct.image.ImageBase)} and needs
	 * to have the pyramid scales set manually.
	 */
	public SubsamplePyramid() {
		super(false,null);
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
		this.scale = scaleFactors.clone();
		checkScales();
		layers = null;
	}

	@Override
	public double getScale(int layer) {
		return scale[layer];
	}

	@Override
	public int getNumLayers() {
		return scale.length;
	}

	@Override
	public void declareLayers(ImageGenerator<T> generator, int width , int height ) {
		this.bottomWidth = width;
		this.bottomHeight = height;

		Class<T> type = generator.getType();

		layers = (T[]) Array.newInstance(type, scale.length);
		double scaleFactor = scale[0];

		if (scaleFactor == 1) {
			if (!saveOriginalReference) {
				layers[0] = generator.createInstance(bottomWidth, bottomHeight);
			}
		} else {
			int w = scaleLength(bottomWidth, scaleFactor);
			int h = scaleLength(bottomHeight, scaleFactor);
			layers[0] = generator.createInstance( w , h );
		}

		for (int i = 1; i < scale.length; i++) {
			int w = scaleLength(bottomWidth, scale[i]);
			int h = scaleLength(bottomHeight, scale[i]);
			layers[i] = generator.createInstance(w,h);
		}
	}

	private int scaleLength( int length , double scaleFactor ) {
		return (int)Math.ceil(length/scaleFactor);
	}

	public double[] getScaleFactors() {
		return scale;
	}
}
