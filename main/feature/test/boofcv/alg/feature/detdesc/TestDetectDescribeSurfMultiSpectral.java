/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detdesc;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.DescribePointSurfMultiSpectral;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDetectDescribeSurfMultiSpectral {

	int width = 200;
	int height = 250;

	Random rand = new Random(234);

	@Test
	public void basicTest() {
		MultiSpectral<ImageFloat32> input = new MultiSpectral<ImageFloat32>(ImageFloat32.class,width,height,3);

		GImageMiscOps.addUniform(input, rand, 0, 200);

		DescribePointSurf<ImageFloat32> desc = new DescribePointSurf<ImageFloat32>(ImageFloat32.class);
		DescribePointSurfMultiSpectral<ImageFloat32> descMulti = new DescribePointSurfMultiSpectral<ImageFloat32>(desc,3);

		FastHessianFeatureDetector<ImageFloat32> detector = FactoryInterestPointAlgs.fastHessian(null);
		OrientationIntegral<ImageFloat32> orientation = FactoryOrientationAlgs.sliding_ii(null, ImageFloat32.class);

		DetectDescribeSurfMultiSpectral<ImageFloat32> alg =
				new DetectDescribeSurfMultiSpectral<ImageFloat32>(detector,orientation,descMulti);

		ImageFloat32 gray = ConvertImage.average(input, null);

		// see if it detects the same number of points
		detector.detect(gray);
		int expected = detector.getFoundPoints().size();

		alg.detect(gray,input);

		assertEquals(expected,alg.getNumberOfFeatures());

		// could improve this unit test by checking scale and orientation
	}
}
