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

package boofcv.alg.transform.pyramid;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;


/**
 * <p>
 * Updates each layer in a {@link boofcv.struct.pyramid.PyramidFloat} by rescaling the layer with interpolation.
 * Unlike {@link PyramidFloatGaussianScale}, no additional blurring is done between layers.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidFloatScale< T extends ImageGray>
		extends PyramidFloat<T> {

	// interpolation algorithm
	protected InterpolatePixelS<T> interpolate;

	public PyramidFloatScale(InterpolatePixelS<T> interpolate, double scaleFactors[] , Class<T> imageType) {
		super(imageType,scaleFactors);
		this.interpolate = interpolate;
	}

	@Override
	public void process(T input) {
		super.initialize(input.width,input.height);
		
		if( isSaveOriginalReference() )
			throw new IllegalArgumentException("The original reference cannot be saved");

		for( int i = 0; i < scale.length; i++ ) {
			T prev = i == 0 ? input : getLayer(i-1);
			T layer = getLayer(i);

			PixelTransformAffine_F32 model = DistortSupport.transformScale(layer,prev, null);
			DistortImageOps.distortSingle(prev,layer, true, model,interpolate);
		}
	}

	@Override
	public double getSampleOffset(int layer) {
		return 0;
	}

	/**
	 * No blur is applied to the image
	 *
	 * @param layer Layer in the pyramid
	 * @return scale
	 */
	@Override
	public double getSigma(int layer) {
		return 0;
	}
}
