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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSiftDetector extends GenericFeatureDetector {

	SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f, 5, 4, false);

	public TestSiftDetector() {
		this.scaleTolerance = 0.3;
	}

	@Override
	protected Object createDetector(int maxFeatures) {
		ConfigSiftDetector config = new ConfigSiftDetector();
		config.maxFeaturesPerScale = maxFeatures;
		return FactoryInterestPointAlgs.siftDetector(config);
	}

	@Override
	protected int detectFeature(ImageFloat32 input, Object detector) {
		SiftDetector alg = (SiftDetector)detector;

		ss.constructPyramid(input);
		ss.computeFeatureIntensity();

		alg.process(ss);

		return alg.getFoundPoints().size;
	}

	/**
	 * Makes sure the blob color flag is being set.  Doesn't check to see if it is being set correctly
	 * since that is more difficult to implement
	 */
	@Test
	public void checkWhiteBlack() {
		ImageFloat32 input = new ImageFloat32(width,height);

		// render a checkered pattern
		renderCheckered(input);

		SiftDetector alg = FactoryInterestPointAlgs.siftDetector(null);

		ss.constructPyramid(input);
		ss.computeFeatureIntensity();

		alg.process(ss);

		List<ScalePoint> l = alg.getFoundPoints().toList();

		assertTrue(l.size()>1);

		int countWhite = 0;
		int countBlack = 0;
		for( ScalePoint s : l ) {
			if( s.isWhite() )
				countWhite++;
			else
				countBlack++;
		}

		assertTrue(countWhite>0);
		assertTrue(countBlack>0);
	}
}
