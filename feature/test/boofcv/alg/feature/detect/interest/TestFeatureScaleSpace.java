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

package boofcv.alg.feature.detect.interest;

import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I32;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestFeatureScaleSpace extends GenericFeatureScaleDetector {

	@Override
	protected Object createDetector(GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector) {
		return new FeatureScaleSpace<ImageFloat32,ImageFloat32>(detector,2);
	}

	@Override
	protected List<Point2D_I32> detectFeature(ImageFloat32 input, double[] scales, Object detector) {
		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();
		ss.setScales(scales);
		ss.setImage(input);

		FeatureScaleSpace<ImageFloat32,ImageFloat32> alg =
				(FeatureScaleSpace<ImageFloat32,ImageFloat32>)detector;
		alg.detect(ss);

		return (List)alg.getInterestPoints();
	}
}