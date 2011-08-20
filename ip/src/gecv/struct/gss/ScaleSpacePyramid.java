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

package gecv.struct.gss;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.transform.gss.PyramidUpdateGaussianScale;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.SubsamplePyramid;


/**
 * <p>
 * A pyramidal representation of {@link gecv.struct.gss.GaussianScaleSpace scale-space}.
 * </p>
 * 
 * <p>
 * NOTE:  Internally the scale factors have been divided by two so that each layer in this
 * pyramid will be comparable to a layer in {@link gecv.struct.gss.GaussianScaleSpace} at the same
 * scale.  This has the side affect of enlarging layers when the scale is < 2.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class ScaleSpacePyramid<T extends ImageBase> extends SubsamplePyramid<T> {


	public ScaleSpacePyramid(InterpolatePixel<T> interpolate, double... scaleFactors) {
		super(new PyramidUpdateGaussianScale(interpolate), scaleFactors);
	}

	public ScaleSpacePyramid() {
	}

	/**
	 * Sets the scale space for the pyramid.  Internally the scales are divided by two.
	 *
	 * @param scaleFactors Change in scale factor for each layer in the pyramid.
	 */
	@Override
	public void setScaleFactors(double... scaleFactors) {
		scaleFactors = scaleFactors.clone();
		for( int i = 0; i < scaleFactors.length; i++ ) {
			scaleFactors[i] *= 0.5;
		}
		super.setScaleFactors(scaleFactors);
	}

	@Override
	public double getScale(int layer) {
		return scale[layer]*2;
	}
}
