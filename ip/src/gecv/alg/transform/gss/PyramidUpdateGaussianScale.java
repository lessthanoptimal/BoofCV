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

package gecv.alg.transform.gss;

import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.abst.filter.blur.impl.BlurStorageFilter;
import gecv.abst.filter.distort.GeneralizedDistortImageOps;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;


/**
 * <p>
 * {@link PyramidUpdater> for {@link ImagePyramid}s where each layer is first blurred using
 * a Gaussian filter and then downsampled using interpolation.  The scaling factor between
 * each level is a floating point number and the Gaussian blur's sigma is set to this value.
 * </p>
 *
 * <p>
 * NOTE: Many would regard this as the theoretically correct wayto construct an image pyramid
 * with no sacrifices for speed.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidUpdateGaussianScale< T extends ImageBase> implements PyramidUpdater<T> {

	// interpolation algorithm
	protected InterpolatePixel<T> interpolate;

	// used to store the blurred image
	protected T tempImage;

	public PyramidUpdateGaussianScale(InterpolatePixel<T> interpolate) {
		this.interpolate = interpolate;
	}

	@Override
	public void update(T input, ImagePyramid<T> _imagePyramid) {
		if( _imagePyramid.saveOriginalReference )
			throw new IllegalArgumentException("The original reference cannot be saved");
		
		ScaleSpacePyramid<T> imagePyramid = (ScaleSpacePyramid<T>)_imagePyramid;

		if( tempImage == null ) {
			tempImage = (T)input._createNew(input.width,input.height);
		}

		for( int i = 0; i < imagePyramid.scale.length; i++ ) {
			T prev = i == 0 ? input : imagePyramid.getLayer(i-1);
			T layer = imagePyramid.getLayer(i);

			float s;
			if( i > 0 )
				s = (float)(imagePyramid.scale[i]/imagePyramid.scale[i-1]);
			else
				s = (float)imagePyramid.scale[0];

			BlurStorageFilter<T> blur = (BlurStorageFilter<T>)FactoryBlurFilter.gaussian(layer.getClass(),s,-1);

			tempImage.reshape(prev.width,prev.height);
			blur.process(prev,tempImage);

			GeneralizedDistortImageOps.scale(tempImage,layer,interpolate);
		}
	}
}
