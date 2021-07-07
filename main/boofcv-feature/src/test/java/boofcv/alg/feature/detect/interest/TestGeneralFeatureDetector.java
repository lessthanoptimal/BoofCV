/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.feature.detect.selector.FeatureSelectNBest;
import boofcv.alg.feature.detect.selector.SampleIntensityImage;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I16;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Peter Abeles
 */
class TestGeneralFeatureDetector extends BoofStandardJUnit {

	int width = 10;
	int height = 12;
	FeatureSelectLimitIntensity<Point2D_I16> selector = new FeatureSelectNBest<>(new SampleIntensityImage.I16());

	/**
	 * Several basic detection tests
	 */
	@Test
	void basics() {
		// use a real extractor
		NonMaxSuppression extractorMin,extractorMax;

		HelperIntensity intensity = new HelperIntensity(false, false, false);

		// add several features while avoiding the image border
		intensity.img.set(1, 1, 10);
		intensity.img.set(7, 1, 10);
		intensity.img.set(1, 5, 10);
		intensity.img.set(7, 5, 10);
		intensity.img.set(1, 9, 10);
		intensity.img.set(6, 9, 10);

		// add a couple of minimums
		intensity.img.set(2, 5, -10);
		intensity.img.set(6, 5, -10);

		// configure it to only detect positive features
		intensity.minimums = false;
		extractorMax = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true, false, true));
		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, null,extractorMax, selector);
		detector.process(new GrayF32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(0, detector.getMinimums().size());

		// try detecting the negative features too
		intensity.minimums = true;
		extractorMin = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true, true, false));
		detector = new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);
		detector.process(new GrayF32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());

		// call it twice to make sure everything is reset correctly
		detector.process(new GrayF32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());
	}

	/**
	 * If the intensity image has an ignore border is that border request actually followed?
	 */
	@Test
	void ignoreBorder() {
		HelperIntensity intensity = new HelperIntensity(false, false, false);
		intensity.ignoreBorder = 2;
		intensity.minimums = true;

		// add several features inside the image
		intensity.img.set(3, 3, 10);
		intensity.img.set(5, 5, 10);
		intensity.img.set(6, 7, 10);
		intensity.img.set(4, 7, -10);
		intensity.img.set(6, 8, -10);

		// add some along the border
		intensity.img.set(5, 1, 10);
		intensity.img.set(5, 11, 10);
		intensity.img.set(0,5 , 10);
		intensity.img.set(1,1 , 10);
		intensity.img.set(9,5 , 10);
		intensity.img.set(9,4 , -10);

		// use a real extractor
		NonMaxSuppression extractorMin = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true,true,false));
		NonMaxSuppression extractorMax = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true,false,true));

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);
		detector.process(new GrayF32(width, height), null, null, null, null, null);

		// only features inside the image should be found
		assertEquals(3, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());
	}

	@Test
	void testPositiveNoCandidates() {
		HelperExtractor extractor = new HelperExtractor(false, true);
		HelperIntensity intensity = new HelperIntensity(false, false, false);

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, null,extractor, selector);

		detector.process(new GrayF32(width, height), null, null, null, null, null);

		assertEquals(0, intensity.candidatesMinCalled);
		assertEquals(0, intensity.candidatesMaxCalled);
		assertEquals(1, intensity.processCalled);
		assertEquals(1, extractor.numTimesProcessed);
	}

	@Test void testPositiveCandidates() {
		testPositiveCandidates(true,false);
		testPositiveCandidates(false,true);
		testPositiveCandidates(true,true);
	}

	public void testPositiveCandidates( boolean min , boolean max ) {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		intensity.minimums = min;
		intensity.maximums = max;
		HelperExtractor extractorMin = null;
		if( min ) {
			extractorMin = new HelperExtractor(true, true);
			extractorMin.minimums = true;
			extractorMin.maximums = false;
		}
		HelperExtractor extractorMax = null;
		if( max ) {
			extractorMax = new HelperExtractor(true, true);
			extractorMax.minimums = false;
			extractorMax.maximums = true;
		}
		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);

		detector.process(new GrayF32(width, height), null, null, null, null, null);

		//  since candidates returns null if not supported it is still ok for it to be invoked
		if( min )
			assertEquals(1, intensity.candidatesMinCalled);
		if( max )
			assertEquals(1, intensity.candidatesMaxCalled);
		assertEquals(1, intensity.processCalled);
		if( min )
			assertEquals(1, extractorMin.numTimesProcessed);
		if( max )
			assertEquals(1, extractorMax.numTimesProcessed);
	}

	/**
	 * If an extractor requires candidates the intensity image needs to provide them.
	 */
	@Test void candidatesMissMatch() {
		HelperIntensity intensity = new HelperIntensity(false, false, false);
		HelperExtractor extractor = new HelperExtractor(true, true);

		assertThrows(IllegalArgumentException.class,
				()->new GeneralFeatureDetector<>(intensity, null,extractor, selector));
	}

	/**
	 * If n-best is not used then the corner list from the extractor should be returned.
	 */
	@Test void testNoNBestSelect() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, null,extractor, selector);

		detector.process(new GrayF32(width, height), null, null, null, null, null);

		// two features are added by the extractor
		assertEquals(2, detector.getMaximums().size());
	}

	/**
	 * See if n-best is used to prune features.
	 */
	@Test void testWithNBestSelect() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, null,extractor, selector);
		detector.setFeatureLimit(1);

		detector.process(new GrayF32(width, height), null, null, null, null, null);

		// it should select only one of the two features to return
		assertEquals(1, detector.getMaximums().size());
	}

	/**
	 * If n-best wasn't initially being used it should now be used
	 */
	@Test void setBestNumber() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity,null, extractor, selector);

		detector.process(new GrayF32(width, height), null, null, null, null, null);

		// two features are added by the extractor
		assertEquals(2, detector.getMaximums().size());
		// it should now create an n-best select
		detector.setFeatureLimit(1);
		detector.process(new GrayF32(width, height), null, null, null, null, null);
		assertEquals(1, detector.getMaximums().size());
		// it should now return all two features
		detector.setFeatureLimit(2);
		detector.process(new GrayF32(width, height), null, null, null, null, null);
		assertEquals(2, detector.getMaximums().size());
	}

	/**
	 * Makes sure flags that indicate the presence of local minimums and maximums are handled correctly
	 */
	@Test void handleLocalMinMaxFlags() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractorMin = new HelperExtractor(true, true);
		extractorMin.minimums = true;
		extractorMin.maximums = false;
		HelperExtractor extractorMax = new HelperExtractor(true, true);
		extractorMax.minimums = false;
		extractorMax.maximums = true;

		intensity.minimums = false;
		intensity.maximums = false;

		GeneralFeatureDetector<GrayF32, GrayF32> detector =
				new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);

		assertFalse(detector.isDetectMinimums());
		assertFalse(detector.isDetectMaximums());

		intensity.minimums = true;
		intensity.maximums = false;

		detector = new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);

		assertTrue(detector.isDetectMinimums());
		assertFalse(detector.isDetectMaximums());

		intensity.minimums = false;
		intensity.maximums = true;

		detector = new GeneralFeatureDetector<>(intensity, extractorMin, extractorMax, selector);

		assertFalse(detector.isDetectMinimums());
		assertTrue(detector.isDetectMaximums());
	}

	public static class HelperExtractor implements NonMaxSuppression {

		boolean usesCandidates;
		boolean acceptsRequests;

		public int numTimesProcessed;

		public boolean minimums = false;
		public boolean maximums = true;

		public HelperExtractor(boolean usesCandidates, boolean acceptsRequests) {
			this.usesCandidates = usesCandidates;
			this.acceptsRequests = acceptsRequests;
		}

		@Override
		public void process(GrayF32 intensity,
							ListIntPoint2D candidateMin, ListIntPoint2D candidateMax,
							QueueCorner foundMin, QueueCorner foundMax) {
			numTimesProcessed++;

			if( foundMin != null ) {
				foundMin.append(1, 1);
				foundMin.append(2, 2);
			}
			if( foundMax != null ) {
				foundMax.append(1, 1);
				foundMax.append(2, 2);
			}
		}

		@Override
		public boolean getUsesCandidates() {
			return usesCandidates;
		}

		@Override
		public float getThresholdMinimum() {
			return 0;
		}

		@Override
		public float getThresholdMaximum() {
			return 0;
		}

		@Override
		public void setThresholdMinimum(float threshold) {
		}

		@Override
		public void setThresholdMaximum(float threshold) {
		}

		@Override
		public int getIgnoreBorder() {
			return 0;
		}

		@Override
		public void setIgnoreBorder(int border) {
		}

		@Override
		public void setSearchRadius(int radius) {
		}

		@Override
		public int getSearchRadius() {
			return 0;
		}

		@Override
		public boolean canDetectMinimums() {
			return minimums;
		}

		@Override
		public boolean canDetectMaximums() {
			return maximums;
		}
	}

	public class HelperIntensity implements GeneralFeatureIntensity<GrayF32, GrayF32> {
		boolean requiresGradient;
		boolean requiresHessian;
		boolean hasCandidates;

		public ListIntPoint2D candidates = new ListIntPoint2D();
		public int processCalled = 0;
		public int candidatesMinCalled = 0;
		public int candidatesMaxCalled = 0;
		public int ignoreBorder = 0;
		public boolean minimums = false;
		public boolean maximums = true;

		public GrayF32 img = new GrayF32(width, height);

		public HelperIntensity(boolean requiresGradient, boolean requiresHessian, boolean hasCandidates) {
			this.requiresGradient = requiresGradient;
			this.requiresHessian = requiresHessian;
			this.hasCandidates = hasCandidates;
		}

		@Override
		public void process(GrayF32 image, GrayF32 derivX, GrayF32 derivY, GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY) {
			processCalled++;
		}

		@Override
		public GrayF32 getIntensity() {
			return img;
		}

		@Override
		public ListIntPoint2D getCandidatesMin() {
			candidatesMinCalled++;
			return candidates;
		}

		@Override
		public ListIntPoint2D getCandidatesMax() {
			candidatesMaxCalled++;
			return candidates;
		}

		@Override
		public boolean getRequiresGradient() {
			return requiresGradient;
		}

		@Override
		public boolean getRequiresHessian() {
			return requiresHessian;
		}

		@Override
		public boolean hasCandidates() {
			return hasCandidates;
		}

		@Override
		public int getIgnoreBorder() {
			return ignoreBorder;
		}

		@Override
		public boolean localMaximums() {
			return maximums;
		}

		@Override
		public Class<GrayF32> getImageType() {
			return GrayF32.class;
		}

		@Override
		public Class<GrayF32> getDerivType() {
			return GrayF32.class;
		}

		@Override
		public boolean localMinimums() {
			return minimums;
		}
	}
}
