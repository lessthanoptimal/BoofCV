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

package boofcv.io.fiducial;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestFiducialIO extends BoofStandardJUnit {
	@Test
	void uchiya_yaml() {
		var expected = new RandomDotDefinition();
		expected.randomSeed = 99494;
		expected.maxDotsPerMarker = 93;
		expected.dotDiameter = 3.4;
		expected.units = "bite me";
		expected.markerWidth = 100;
		expected.markerHeight = 120;

		for (int i = 0; i < 2; i++) {
			expected.markers.add(UtilPoint2D_F64.random(-1, 1, 10 + i, rand));
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		FiducialIO.saveRandomDotYaml(expected, new OutputStreamWriter(stream, UTF_8));

		Reader reader = new InputStreamReader(new ByteArrayInputStream(stream.toByteArray()), UTF_8);
		RandomDotDefinition found = FiducialIO.loadRandomDotYaml(reader);

		assertEquals(expected.randomSeed, found.randomSeed);
		assertEquals(expected.maxDotsPerMarker, found.maxDotsPerMarker);
		assertEquals(expected.dotDiameter, found.dotDiameter);
		assertEquals(expected.units, found.units);
		assertEquals(expected.markerWidth, found.markerWidth);
		assertEquals(expected.markerHeight, found.markerHeight);

		assertEquals(expected.markers.size(), found.markers.size());

		for (int i = 0; i < 2; i++) {
			List<Point2D_F64> a = expected.markers.get(i);
			List<Point2D_F64> b = found.markers.get(i);

			assertEquals(a.size(), b.size());

			for (int j = 0; j < a.size(); j++) {
				assertEquals(0, a.get(j).distance(b.get(j)), UtilEjml.TEST_F64);
			}
		}
	}
}
