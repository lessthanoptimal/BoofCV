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

package boofcv.alg.background.stationary;

import boofcv.BoofTesting;
import boofcv.alg.background.BackgroundModelStationary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Peter Abeles
 */
public abstract class CompareBackgroundStationaryThreadsChecks extends BoofStandardJUnit {
	int width = 120;
	int height = 90;

	protected List<ImageType<?>> imageTypes = new ArrayList<>();

	public abstract <T extends ImageBase<T>> BackgroundModelStationary<T> create( boolean singleThread, ImageType<T> imageType );

	/**
	 * Go through different image types and make sure the two algorithms have the same results
	 */
	@Test void identicalResults() {
		// sanity check to make sure imageTypes have been specified. Otherwise, this could pass and do nothing
		assertFalse(imageTypes.isEmpty());

		for (ImageType<?> type : imageTypes) {
			identicalResults(type);
		}
	}

	private <T extends ImageBase<T>> void identicalResults( ImageType<T> imageType ) {
		BackgroundModelStationary<T> single = create(true, imageType);
		BackgroundModelStationary<T> multi = create(false, imageType);

		T frame = imageType.createImage(width, height);

		for (int i = 0; i < 30; i++) {
			noise(100, 20, frame);
			single.updateBackground(frame);
			multi.updateBackground(frame);
		}

		int x0 = 10, y0 = 12, x1 = 40, y1 = 38;

		noise(100, 20, frame);
		GImageMiscOps.fillRectangle(frame, 200, x0, y0, x1 - x0, y1 - y0);

		GrayU8 foundSingle = new GrayU8(width, height);
		GrayU8 foundMulti = new GrayU8(width, height);

		single.segment(frame, foundSingle);
		multi.segment(frame, foundMulti);

		BoofTesting.assertEquals(foundSingle, foundMulti, 0);

		// Sanity check to make sure results are not all 0 or 1
		double average = GImageStatistics.sum(foundMulti)/foundMulti.totalPixels();
		assertNotEquals(0.0, average, UtilEjml.TEST_F64);
		assertNotEquals(1.0, average, UtilEjml.TEST_F64);
	}

	protected void noise( double mean, double range, ImageBase image ) {
		GImageMiscOps.fill(image, mean);
		GImageMiscOps.addUniform(image, rand, -range, range);
	}
}
