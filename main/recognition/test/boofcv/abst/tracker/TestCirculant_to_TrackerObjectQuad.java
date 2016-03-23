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

package boofcv.abst.tracker;

import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestCirculant_to_TrackerObjectQuad extends TextureGrayTrackerObjectRectangleTests {

	public TestCirculant_to_TrackerObjectQuad() {
		tolStationary = 1;
	}

	@Override
	public TrackerObjectQuad<GrayU8> create(ImageType<GrayU8> imageType) {

		ConfigCirculantTracker config = new ConfigCirculantTracker();

		return FactoryTrackerObjectQuad.circulant(config, GrayU8.class);
	}

	@Test
	public void zooming_in() {
		// not supported
	}

	@Test
	public void zooming_out() {
		// not supported
	}
}
