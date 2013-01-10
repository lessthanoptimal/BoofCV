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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestGeneralFeatureDetector {

	int width = 10;
	int height = 12;

	/**
	 * Several basic detection tests
	 */
	@Test
	public void testDetection() {
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

		// use a real extractor
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true));

		// configure it to only detect positive features
		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(0, detector.getMinimums().size());

		// try detecting the negative features too
		detector = new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,true);
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());

		// call it twice to make sure everything is reset correctly
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);
		assertEquals(6, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());
	}

	/**
	 * If the intensity image has an ignore border is that border request actually followed?
	 */
	@Test
	public void ignoreBorder() {
		HelperIntensity intensity = new HelperIntensity(false, false, false);
		intensity.ignoreBorder = 2;

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
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(1, 0.001f, 1, true));

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,true);
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		// only features inside the image should be found
		assertEquals(3, detector.getMaximums().size());
		assertEquals(2, detector.getMinimums().size());
	}

	@Test
	public void testPositiveNoCandidates() {
		HelperExtractor extractor = new HelperExtractor(false, true);
		HelperIntensity intensity = new HelperIntensity(false, false, false);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		assertTrue(intensity.candidatesCalled == 0);
		assertTrue(intensity.processCalled == 1);
		assertTrue(extractor.numTimesProcessed == 1);
	}

	@Test
	public void testPositiveCandidates() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		assertTrue(intensity.candidatesCalled == 1);
		assertTrue(intensity.processCalled == 1);
		assertTrue(extractor.numTimesProcessed == 1);
	}

	/**
	 * If an extractor requires candidates the intensity image needs to provide them.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void candidatesMissMatch() {
		HelperIntensity intensity = new HelperIntensity(false, false, false);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		fail("exception should have been thrown");
	}

	/**
	 * If n-best is not used then the corner list from the extractor should be returned.
	 */
	@Test
	public void testNoNBestSelect() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		// two features are added by the extractor
		assertEquals(2, detector.getMaximums().size());
	}

	/**
	 * See if n-best is used to prune features.
	 */
	@Test
	public void testWithNBestSelect() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);
		detector.setMaxFeatures(1);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		// it should select only one of the two features to return
		assertEquals(1, detector.getMaximums().size());
	}

	/**
	 * If n-best wasn't initially being used it should now be used
	 */
	@Test
	public void setBestNumber() {
		HelperIntensity intensity = new HelperIntensity(false, false, true);
		HelperExtractor extractor = new HelperExtractor(true, true);

		GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32, ImageFloat32>(intensity, extractor,false);

		detector.process(new ImageFloat32(width, height), null, null, null, null, null);

		// two features are added by the extractor
		assertEquals(2, detector.getMaximums().size());
		// it should now create an n-best select
		detector.setMaxFeatures(1);
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);
		assertEquals(1, detector.getMaximums().size());
		// it should now return all two features
		detector.setMaxFeatures(2);
		detector.process(new ImageFloat32(width, height), null, null, null, null, null);
		assertEquals(2, detector.getMaximums().size());
	}


	public class HelperExtractor implements FeatureExtractor {

		boolean usesCandidates;
		boolean acceptsRequests;

		public int numTimesProcessed;

		public HelperExtractor(boolean usesCandidates, boolean acceptsRequests) {
			this.usesCandidates = usesCandidates;
			this.acceptsRequests = acceptsRequests;
		}

		@Override
		public void process(ImageFloat32 intensity, QueueCorner candidate, QueueCorner foundFeature) {
			numTimesProcessed++;

			foundFeature.add(1, 1);
			foundFeature.add(2, 2);
		}

		@Override
		public boolean getUsesCandidates() {
			return usesCandidates;
		}

		@Override
		public float getThreshold() {
			return 0;
		}

		@Override
		public void setThreshold(float threshold) {
		}

		@Override
		public int getIgnoreBorder() {
			return 0;
		}

		@Override
		public void setIgnoreBorder(int border) {
		}

		@Override
		public boolean canDetectBorder() {
			return true;
		}

		@Override
		public void setSearchRadius(int radius) {
		}

		@Override
		public int getSearchRadius() {
			return 0;
		}
	}

	public class HelperIntensity implements GeneralFeatureIntensity<ImageFloat32, ImageFloat32> {
		boolean requiresGradient;
		boolean requiresHessian;
		boolean hasCandidates;

		public QueueCorner candidates = new QueueCorner(10);
		public int processCalled = 0;
		public int candidatesCalled = 0;
		public int ignoreBorder = 0;

		public ImageFloat32 img = new ImageFloat32(width, height);

		public HelperIntensity(boolean requiresGradient, boolean requiresHessian, boolean hasCandidates) {
			this.requiresGradient = requiresGradient;
			this.requiresHessian = requiresHessian;
			this.hasCandidates = hasCandidates;
		}

		@Override
		public void process(ImageFloat32 image, ImageFloat32 derivX, ImageFloat32 derivY, ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY) {
			processCalled++;
		}

		@Override
		public ImageFloat32 getIntensity() {
			return img;
		}

		@Override
		public QueueCorner getCandidates() {
			candidatesCalled++;
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
	}
}
