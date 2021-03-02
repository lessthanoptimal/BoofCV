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

package boofcv.io.calibration;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationIO extends BoofStandardJUnit {
	@Test void landmarksCSV() {
		var original = new CalibrationObservation();
		original.width = 99;
		original.height = 123;
		original.add(1, 2, -23);
		original.add(5, 10.5, -23);
		original.add(3.11, -20.1, -23);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CalibrationIO.saveLandmarksCsv("File","ASDASD",original, stream);

		CalibrationObservation found = CalibrationIO.loadLandmarksCsv(new ByteArrayInputStream(stream.toByteArray()));

		assertEquals(original.width, found.width);
		assertEquals(original.height, found.height);
		assertEquals(original.points.size(), found.points.size());
		for (int i = 0; i < original.points.size(); i++) {
			PointIndex2D_F64 o = original.get(i);
			PointIndex2D_F64 f = found.get(i);

			assertEquals(o.index, f.index);
			assertEquals(o.p.x, f.p.x);
			assertEquals(o.p.y, f.p.y);
		}
	}
}
