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

package boofcv.alg.background.moving;

import boofcv.BoofTesting;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.homography.Homography2D_F32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericBackgroundMovingThreadsChecks extends BoofStandardJUnit {
	int width = 200;
	int height = 150;


	protected List<ImageType> imageTypes = new ArrayList<>();

	public abstract <T extends ImageBase<T>>
	BackgroundModelMoving<T, Homography2D_F32> create( boolean singleThread, ImageType<T> imageType );

	/**
	 * Sees if multi and single thread implementations produce identical results
	 */
	@Test void compareToSingle() {
		for (ImageType type : imageTypes) {
			compareToSingle(type);
		}
	}

	private <T extends ImageBase<T>> void compareToSingle( ImageType<T> imageType ) {
		BackgroundModelMoving<T, Homography2D_F32> single = create(true, imageType);
		BackgroundModelMoving<T, Homography2D_F32> multi = create(false, imageType);


		T frame = imageType.createImage(width, height);

		Homography2D_F32 homeToWorld = new Homography2D_F32(1, 0, width/2, 0, 1, height/2, 0, 0, 1);

		single.initialize(width*2, height*2, homeToWorld);
		multi.initialize(width*2, height*2, homeToWorld);

		for (int i = 0; i < 30; i++) {
			var homeToCurrent = new Homography2D_F32();
			if (i > 0) {
				homeToCurrent.a13 = rand.nextFloat()*5 - 2.5f;
				homeToCurrent.a23 = rand.nextFloat()*5 - 2.5f;
			}
			noise(100, 2, frame);

			single.updateBackground(new Homography2D_F32(), frame);
			multi.updateBackground(new Homography2D_F32(), frame);
		}

		int x0 = 10, y0 = 12, x1 = 40, y1 = 38;

		noise(100, 2, frame);
		GImageMiscOps.fillRectangle(frame, 200, x0, y0, x1 - x0, y1 - y0);

		var homeToCurrent = new Homography2D_F32();
		var expected = new GrayU8(width, height);
		var found = new GrayU8(width, height);

		single.segment(homeToCurrent, frame, expected);
		multi.segment(homeToCurrent, frame, found);

		BoofTesting.assertEquals(expected, found, 0);
	}

	private void noise( double mean, double range, ImageBase image ) {
		GImageMiscOps.fill(image, mean);
		GImageMiscOps.addUniform(image, rand, -range, range);
	}
}
