/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayS32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class TestConvertLabeledImageFormats extends BoofStandardJUnit {
	/**
	 * Tested using a simple rectangle
	 */
	@Test void rectangle() {
		List<PolygonRegion> regions = new ArrayList<>();
		var r = new PolygonRegion();
		regions.add(r);
		r.polygon.vertexes.grow().setTo(5,5);
		r.polygon.vertexes.grow().setTo(15,5);
		r.polygon.vertexes.grow().setTo(15,20);
		r.polygon.vertexes.grow().setTo(5,20);
		r.regionID = 3;

		var expected = new GrayS32(30,35);
		GImageMiscOps.fillRectangle(expected,3,5,5,10,15);

		GrayS32 found = ConvertLabeledImageFormats.convert(regions, expected.width, expected.height, null);

		BoofTesting.assertEquals(expected, found, 1e-8);
	}
}
