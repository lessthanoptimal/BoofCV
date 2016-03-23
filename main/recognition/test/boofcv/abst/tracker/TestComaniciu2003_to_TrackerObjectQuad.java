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
import boofcv.struct.image.Planar;

/**
 * @author Peter Abeles
 */
public class TestComaniciu2003_to_TrackerObjectQuad extends ColorTrackerObjectRectangleTests {

	public TestComaniciu2003_to_TrackerObjectQuad() {
		super(true);
		tolTranslateSmall = 0.05;
		tolScale = 0.1;
	}

	@Override
	public TrackerObjectQuad<Planar<GrayU8>> create(ImageType<Planar<GrayU8>> imageType) {
		ConfigComaniciu2003 config = new ConfigComaniciu2003();

		config.scaleChange = 0.1f;

		return FactoryTrackerObjectQuad.meanShiftComaniciu2003(config,imageType);
	}

	@Override
	public void translation_large() {
		// Not designed to handle
	}
}
