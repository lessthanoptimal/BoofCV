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

package boofcv.alg.transform.pyramid;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.pyramid.PyramidFloat;
import boofcv.struct.pyramid.PyramidUpdaterFloat;


/**
 * <p>
 * Updates each layer in a {@link boofcv.struct.pyramid.PyramidFloat} by rescaling the layer with interpolation.
 * Unlike {@link PyramidUpdateGaussianScale}, no additional blurring is done between layers.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidUpdateSubsampleScale< T extends ImageBase>
		implements PyramidUpdaterFloat<T> {

	// interpolation algorithm
	protected InterpolatePixel<T> interpolate;


	public PyramidUpdateSubsampleScale(InterpolatePixel<T> interpolate) {
		this.interpolate = interpolate;
	}

	@Override
	public void update(T input, PyramidFloat<T> pyramid) {
		if( !pyramid.isInitialized() )
			pyramid.initialize(input.width,input.height);
		
		if( pyramid.isSaveOriginalReference() )
			throw new IllegalArgumentException("The original reference cannot be saved");

		for( int i = 0; i < pyramid.scale.length; i++ ) {
			T prev = i == 0 ? input : pyramid.getLayer(i-1);
			T layer = pyramid.getLayer(i);

			float s;
			if( i > 0 )
				s = (float)(pyramid.scale[i]/pyramid.scale[i-1]);
			else
				s = (float)pyramid.scale[0];

			DistortImageOps.scale(prev,layer,interpolate);
		}
	}
}
