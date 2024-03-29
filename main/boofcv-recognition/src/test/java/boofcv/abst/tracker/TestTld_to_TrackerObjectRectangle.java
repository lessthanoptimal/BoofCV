/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

public class TestTld_to_TrackerObjectRectangle extends TextureGrayTrackerObjectRectangleChecks {

	public TestTld_to_TrackerObjectRectangle() {
		tolScale = 0.15;
	}

	@Override
	public TrackerObjectQuad<GrayU8> create(ImageType<GrayU8> imageType) {
		return FactoryTrackerObjectQuad.tld(new ConfigTrackerTld(),GrayU8.class);
	}
}
