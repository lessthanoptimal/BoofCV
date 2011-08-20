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

package gecv.alg.transform.pyramid;

import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;
import gecv.struct.pyramid.SubsamplePyramid;


/**
 * <p>
 * Updates each layer in a {@link SubsamplePyramid} by rescaling the layer with interpolation.
 * Unlike {@link gecv.alg.transform.gss.PyramidUpdateGaussianScale}, no additional blurring is done between layers.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidUpdateSubsampleScale< T extends ImageBase> implements PyramidUpdater<T> {

	// interpolation algorithm
	protected InterpolatePixel<T> interpolate;


	public PyramidUpdateSubsampleScale(InterpolatePixel<T> interpolate) {
		this.interpolate = interpolate;
	}

	@Override
	public void update(T input, ImagePyramid<T> _imagePyramid) {
		if( _imagePyramid.saveOriginalReference )
			throw new IllegalArgumentException("The original reference cannot be saved");
		
		SubsamplePyramid<T> imagePyramid = (SubsamplePyramid<T>)_imagePyramid;

		for( int i = 0; i < imagePyramid.scale.length; i++ ) {
			T prev = i == 0 ? input : imagePyramid.getLayer(i-1);
			T layer = imagePyramid.getLayer(i);

			float s;
			if( i > 0 )
				s = (float)(imagePyramid.scale[i]/imagePyramid.scale[i-1]);
			else
				s = (float)imagePyramid.scale[0];

			DistortImageOps.scale(prev,layer,interpolate);
		}
	}
}
