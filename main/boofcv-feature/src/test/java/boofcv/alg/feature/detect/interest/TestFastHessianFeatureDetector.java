/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFastHessianFeatureDetector extends GenericFeatureDetectorTests {

	public TestFastHessianFeatureDetector() {
		this.scaleTolerance = 0.3;
	}

	@Override
	protected Object createDetector( int maxFeatures ) {
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 1, 5, true));
		FeatureSelectLimitIntensity<Point2D_I16> limitLevels = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
		FeatureSelectLimitIntensity<ScalePoint> limitAll = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
		var alg = new FastHessianFeatureDetector(extractor,limitLevels,limitAll,1, 9,4,4, 6);
		alg.maxFeaturesPerScale = maxFeatures;
		return alg;
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected int detectFeature(GrayF32 input, Object detector) {
		FastHessianFeatureDetector<GrayF32> alg = (FastHessianFeatureDetector<GrayF32>)detector;
		GrayF32 integral = IntegralImageOps.transform(input,null);
		alg.detect(integral);

		return alg.getFoundFeatures().size();
	}

	/**
	 * Computes features inside a random image and sees if there is a reasonable ratio of the two types of features.
	 * This will catch things like it only detecting white or black.
	 */
	@Test
	void areThereBothWhiteAndBlackFeatures() {
		var input = new GrayF32(width, height);
		GImageMiscOps.fillUniform(input,rand,0,255);

		var ii = input.createSameShape();
		IntegralImageOps.transform(input,ii);

		var alg = (FastHessianFeatureDetector<GrayF32>)createDetector(500);

		alg.detect(ii);

		int N = alg.getFoundFeatures().size();
		int countWhite=0;
		for (int i = 0; i < N; i++) {
			if( alg.getFoundFeatures().get(i).isWhite() )
				countWhite++;
		}

		// arbitrary threshold for what can be expected.
		int minimum = N*3/10;
		assertTrue(countWhite>minimum);
		assertTrue((N-countWhite)>minimum);
	}

	/**
	 * Sees if fewer points are selected when max per level is adjusted
	 */
	@Test
	void respondsToMaxPerLevel() {
		// no limit to detection
		var detector = (FastHessianFeatureDetector)createDetector(-1);

		var input = new GrayF32(width, height);
		GImageMiscOps.fillUniform(input,rand,0,255);
		var ii = input.createSameShape();
		IntegralImageOps.transform(input,ii);

		detector.detect(ii);
		int countUnlimited = detector.getFoundFeatures().size();

		// hardly limit it
		detector.maxFeaturesPerScale = 5;
		detector.detect(ii);
		int countLimited = detector.getFoundFeatures().size();
		assertTrue(countLimited>=5);
		assertTrue(countLimited*2 < countUnlimited);
	}

	/**
	 * Will not exceed when the max total is set
	 */
	@Test
	void strictEnforcesMaxTotal() {
		// no limit to detection
		var detector = (FastHessianFeatureDetector)createDetector(-1);

		var input = new GrayF32(width, height);
		GImageMiscOps.fillUniform(input,rand,0,255);
		var ii = input.createSameShape();
		IntegralImageOps.transform(input,ii);

		detector.detect(ii);
		int countUnlimited = detector.getFoundFeatures().size();
		assertTrue(countUnlimited>=30);

		// force it to be a smaller number
		detector.maxFeaturesAll = countUnlimited/2;
		detector.detect(ii);
		int countLimited = detector.getFoundFeatures().size();
		assertEquals(countUnlimited/2,countLimited);

		// force it to be a larger number
		detector.maxFeaturesAll = countUnlimited*2;
		detector.detect(ii);
		countLimited = detector.getFoundFeatures().size();
		assertEquals(countUnlimited,countLimited);
	}
}
