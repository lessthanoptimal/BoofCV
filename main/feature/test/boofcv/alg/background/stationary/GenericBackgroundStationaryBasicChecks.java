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
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundStationaryBasicChecks extends GenericBackgroundModelStationaryChecks {


	@Test
	public void checkLearnRate() {
		for( ImageType type : imageTypes ) {
			checkLearnRate_slow(type);
			checkLearnRate_fast(type);
		}
	}

	private <T extends ImageBase> void checkLearnRate_slow(ImageType<T> imageType) {
		BackgroundStationaryBasic<T> alg = (BackgroundStationaryBasic)create(imageType);
		alg.setThreshold(10);

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
		BackgroundStationaryBasic<T> alg = (BackgroundStationaryBasic)create(imageType);
		alg.setThreshold(10);

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
	public void checkThreshold() {
		for( ImageType type : imageTypes ) {
			checkThreshold(type);
		}
	}

	private <T extends ImageBase> void checkThreshold( ImageType<T> imageType ) {
		BackgroundStationaryBasic<T> alg = (BackgroundStationaryBasic)create(imageType);
		alg.setLearnRate(0.05f);

		T frame = imageType.createImage(width,height);

		// build a background model around these images
		for (int i = 0; i < 5; i++) {
			GImageMiscOps.fill(frame, 100);
			alg.updateBackground(frame);
		}

		GrayU8 segmented = new GrayU8(width,height);

		GImageMiscOps.fill(frame, 103);
		alg.setThreshold(2.5f);
		alg.segment(frame, segmented);
		assertTrue(width * height <= ImageStatistics.sum(segmented));

		alg.setThreshold(3.5f);
		alg.segment(frame, segmented);
		assertTrue(0 == ImageStatistics.sum(segmented));
	}
}
