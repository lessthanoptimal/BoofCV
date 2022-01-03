/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.microqr;

import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMicroQrPose3DUtils extends BoofStandardJUnit {
	@Test void getLandmarkByIndex() {
		var qr = new MicroQrCode();

		// this version has a locator pattern, but we don't care about those. sanity check to see
		// that it's ignored
		qr.version = 2;

		for (int i = 0; i < 4; i++) {
			qr.pp.get(i).setTo(i, i + 1);
		}

		var alg = new MicroQrPose3DUtils();
		List<PointIndex2D_F64> list = alg.getLandmarkByIndex(qr);

		assertEquals(4, list.size());
		for (int i = 0; i < 4; i++) {
			assertEquals(0, list.get(i).p.distance(i, i + 1), UtilEjml.TEST_F64);
		}
	}

	@Test void getLandmark2D3D() {
		var qr = new MicroQrCode();

		// try different versions. This will change the coordinate of points in the marker frame
		for (int version : new int[]{1, 2, 3, 4}) {
			int N = MicroQrCode.totalModules(version);

			// The marker will now have a coordinate system from -1 to 1
			// w is the width of the pp's side in this coordinate system
			double w = 2.0*7.0/(double)N; // coordinate
			qr.version = version;

			for (int i = 0; i < 4; i++) {
				qr.pp.get(i).setTo(i, i + 1);
			}

			MicroQrPose3DUtils alg = createAlg();

			List<Point2D3D> list = alg.getLandmark2D3D(qr);
			assertEquals(4, list.size());
			for (int i = 0; i < 4; i++) {
				// coordinate system is center (0,0) +x right +y up. normalized to have values -1 to 1
				Point3D_F64 X = list.get(i).location;

				assertTrue(X.x >= -1 && X.x <= 1);
				assertTrue(X.y >= -1 && X.y <= 1);
				assertEquals(0, X.z);
				assertEquals(0, list.get(i).observation.distance(0.1*i, 0.1*(i + 1)), UtilEjml.TEST_F64);
			}

			// check a few hand computed points
			assertEquals(0, list.get(0).location.distance(-1, 1, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(1).location.distance(w-1.0, 1, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(2).location.distance(w-1.0, 1-w, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(3).location.distance(-1, 1-w, 0), UtilEjml.TEST_F64);
		}
	}

	/**
	 * Compare answer to what getLandmark2D3D() returns since the info is the same
	 */
	@Test void getLandmark3D() {
		var qr = new MicroQrCode();

		// try different versions. This will change the coordinate of points in the marker frame
		for (int version : new int[]{1, 2, 3, 4}) {
			qr.version = version;

			for (int i = 0; i < 4; i++) {
				qr.pp.get(i).setTo(i, i + 1);
			}

			MicroQrPose3DUtils alg = createAlg();

			List<Point2D3D> expected = alg.getLandmark2D3D(qr);
			List<Point3D_F64> found = alg.getLandmark3D(qr.version);

			assertEquals(expected.size(), found.size());
			for (int i = 0; i < expected.size(); i++) {
				assertEquals(0, expected.get(i).location.distance(found.get(i)), UtilEjml.TEST_F64);
			}
		}
	}

	private MicroQrPose3DUtils createAlg() {
		var alg = new MicroQrPose3DUtils();
		alg.setLensDistortion(new Point2Transform2_F64() {
			@Override
			public void compute( double x, double y, Point2D_F64 out ) {
				// just change the point's scale to make it easy to see if it was applied
				out.x = x*0.1;
				out.y = y*0.1;
			}

			@Override
			public Point2Transform2_F64 copyConcurrent() {
				return null;
			}
		}, new DoNothing2Transform2_F64());
		return alg;
	}
}
