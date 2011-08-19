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

package gecv.alg.feature.detect.interest;

import gecv.abst.detect.point.GeneralFeatureDetector;
import gecv.abst.filter.derivative.AnyImageDerivative;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.transform.gss.PyramidUpdateGaussianScale;
import gecv.alg.transform.gss.UtilScaleSpace;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.feature.ScalePoint;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageFloat32;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestFeaturePyramid extends GenericFeatureScaleDetector {

	@Override
	protected Object createDetector(GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector) {
		AnyImageDerivative<ImageFloat32,ImageFloat32> deriv = UtilScaleSpace.createDerivatives(ImageFloat32.class, FactoryImageGenerator.create(ImageFloat32.class));

		return new FeaturePyramid<ImageFloat32,ImageFloat32>(detector,deriv,1);
	}

	@Override
	protected List<ScalePoint> detectFeature(ImageFloat32 input, double[] scales, Object detector) {
		InterpolatePixel<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		PyramidUpdateGaussianScale<ImageFloat32> update = new PyramidUpdateGaussianScale<ImageFloat32>(interpolate);
		ScaleSpacePyramid<ImageFloat32> ss = new ScaleSpacePyramid<ImageFloat32>(update,scales);
		ss.update(input);

		FeaturePyramid<ImageFloat32,ImageFloat32> alg =
				(FeaturePyramid<ImageFloat32,ImageFloat32>)detector;
		alg.detect(ss);

		return alg.getInterestPoints();
	}

}

