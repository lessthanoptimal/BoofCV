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

package boofcv.alg.background.stationary;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundStationaryGaussianChecks extends GenericBackgroundModelStationaryChecks {

	float initialVariance;

	@Before
	public void init() {
		initialVariance = 12;
	}

	@Test
	public void initialVariance() {
		for( ImageType type : imageTypes ) {
			initialVariance(type);
		}
	}

	private <T extends ImageBase> void initialVariance( ImageType<T> imageType ) {
		this.initialVariance = Float.NaN; // turn off setting it
		BackgroundStationaryGaussian<T> alg = (BackgroundStationaryGaussian)create(imageType);
		alg.setThreshold(10);

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);
		T frame = imageType.createImage(width,height);

		GImageMiscOps.fill(frame,20);
		alg.updateBackground(frame);

		// the initial variance should be zero, which means any small change should be motion
		ImageMiscOps.fill(expected, 0);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);
		ImageMiscOps.fill(expected, 1);
		GImageMiscOps.fill(frame, 21);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);

		// try giving it a larger initial variance
		alg.setInitialVariance(10);
		alg.reset();
		GImageMiscOps.fill(frame, 20);
		alg.updateBackground(frame);

		ImageMiscOps.fill(expected, 0);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);
		GImageMiscOps.fill(frame, 21);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);
		ImageMiscOps.fill(expected, 1);
		GImageMiscOps.fill(frame, 100);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);
	}

	@Test
	public void learnRate() {
		for( ImageType type : imageTypes ) {
			checkLearnRate_slow(type);
			checkLearnRate_fast(type);
		}
	}

	private <T extends ImageBase> void checkLearnRate_slow(ImageType<T> imageType) {
		BackgroundStationaryGaussian<T> alg = (BackgroundStationaryGaussian)create(imageType);
		alg.setThreshold(10);
		alg.setInitialVariance(64);

		T frame = imageType.createImage(width,height);

		// learn very slow.  Should virtually ignore new images with different model
		alg.setLearnRate(0.01f);
		for (int i = 0; i < 30; i++) {
			noise(100, 2, frame);
			alg.updateBackground(frame);
		}
		for (int i = 0; i < 3; i++) {
			noise(150, 2, frame);
			alg.updateBackground(frame);
		}

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		alg.segment(frame,segmented);
		ImageMiscOps.fill(expected, 1);
		BoofTesting.assertEquals(expected,segmented,1e-5f);
	}

	private <T extends ImageBase> void checkLearnRate_fast(ImageType<T> imageType) {
		BackgroundStationaryGaussian<T> alg = (BackgroundStationaryGaussian)create(imageType);
		alg.setThreshold(10);
		alg.setInitialVariance(64);

		T frame = imageType.createImage(width,height);

		// learn very fast.  will quickly discard old images and use the new ones
		alg.setLearnRate(0.99f);
		for (int i = 0; i < 30; i++) {
			noise(100, 2, frame);
			alg.updateBackground(frame);
		}
		for (int i = 0; i < 3; i++) {
			noise(150, 2, frame);
			alg.updateBackground(frame);
		}

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		alg.segment(frame,segmented);
		ImageMiscOps.fill(expected, 0);
		BoofTesting.assertEquals(expected,segmented,1e-5f);
	}

	@Test
	public void minimumDifference() {
		for( ImageType type : imageTypes ) {
			minimumDifference(type);
		}
	}

	private <T extends ImageBase> void minimumDifference( ImageType<T> imageType ) {
		BackgroundStationaryGaussian<T> alg = (BackgroundStationaryGaussian)create(imageType);
		alg.setThreshold(10);
		alg.setInitialVariance(0);

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);
		T frame = imageType.createImage(width,height);

		GImageMiscOps.fill(frame, 20);
		alg.updateBackground(frame);

		// Turn off minimum difference
		alg.setMinimumDifference(0);
		GImageMiscOps.fill(frame, 21);
		alg.segment(frame, segmented);
		ImageMiscOps.fill(expected, 1);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);

		// Turn it on and check
		alg.setMinimumDifference(4);
		GImageMiscOps.fill(frame, 21);
		alg.segment(frame, segmented);
		ImageMiscOps.fill(expected, 0);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);

		GImageMiscOps.fill(frame, 23);
		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);

		GImageMiscOps.fill(frame, 24); // test a value just after the threshold
		alg.segment(frame, segmented);
		ImageMiscOps.fill(expected, 1);
		BoofTesting.assertEquals(expected, segmented, 1e-5f);
	}
}
