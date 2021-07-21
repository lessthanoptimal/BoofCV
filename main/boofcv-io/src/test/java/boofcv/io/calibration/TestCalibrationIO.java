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
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		CalibrationIO.saveLandmarksCsv("File", "ASDASD", original, stream);

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

	/**
	 * Read an actual OpenCV generated file
	 */
	@Test void saveOpenCV() {
		CameraPinholeBrown model = new CameraPinholeBrown();

		model.fsetK(1, 2, 3, 4, 0.65, 100, 7);
		model.fsetRadial(.1, .2, .3);
		model.fsetTangental(0.5, 0.7);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CalibrationIO.saveOpencv(model, new OutputStreamWriter(stream));

		CameraPinholeBrown found = CalibrationIO.loadOpenCV(new InputStreamReader(
				new ByteArrayInputStream(stream.toByteArray())));

		assertEquals(model.width, found.width);
		assertEquals(model.height, found.height);
		assertEquals(model.fx, found.fx, 1e-8);
		assertEquals(model.fy, found.fy, 1e-8);
		assertEquals(model.skew, found.skew, 1e-8);
		assertEquals(model.cx, found.cx, 1e-8);
		assertEquals(model.cy, found.cy, 1e-8);

		for (int i = 0; i < 3; i++) {
			assertEquals(model.radial[i], found.radial[i], 1e-8);
		}

		assertEquals(model.t1, found.t1, 1e-8);
		assertEquals(model.t2, found.t2, 1e-8);
	}

	/**
	 * Read an actual OpenCV generated file
	 */
	@Test void loadOpenCV() {
		URL url = TestCalibrationIO.class.getResource("pinhole_distorted.yml");
		CameraPinholeBrown model = CalibrationIO.loadOpenCV(url);
		assertEquals(640, model.width);
		assertEquals(480, model.height);

		assertEquals(5.2626362816407709e+02, model.fx, 1e-8);
		assertEquals(0, model.skew, 1e-8);
		assertEquals(5.2830704330313858e+02, model.fy, 1e-8);
		assertEquals(3.1306715867053975e+02, model.cx, 1e-8);
		assertEquals(2.4747722332735930e+02, model.cy, 1e-8);

		assertEquals(3, model.radial.length);
		assertEquals(-3.7077854691489726e-01, model.radial[0], 1e-8);
		assertEquals(2.4308561956661329e-01, model.radial[1], 1e-8);
		assertEquals(-1.2351969388838037e-01, model.radial[2], 1e-8);

		assertEquals(4.4148972909019294e-04, model.t1, 1e-8);
		assertEquals(-6.0304229617877381e-04, model.t2, 1e-8);
	}

	@Test void save_load_KannalaBrandt() {
		// try simplified case with only symmetric distortion
		CameraKannalaBrandt model;
		model = new CameraKannalaBrandt().fsetK(500, 550, 0.1, 600, 650);
		model.fsetSymmetric(1.0, 0.4);
		save_load_KannalaBrandt(model);

		// Full distortion model
		model = new CameraKannalaBrandt().fsetK(500, 550, 0.1, 600, 650);
		model.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06, 0.12).
				fsetRadialTrig(0.01, 0.03, -0.03, 0.04).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);
		save_load_KannalaBrandt(model);
	}

	void save_load_KannalaBrandt( CameraKannalaBrandt model ) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CalibrationIO.save(model, new OutputStreamWriter(stream));

		CameraKannalaBrandt found = CalibrationIO.load(new StringReader(stream.toString()));
		assertTrue(model.isIdentical(found));
	}
}
