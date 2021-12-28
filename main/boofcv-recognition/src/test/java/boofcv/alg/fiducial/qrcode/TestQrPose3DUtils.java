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

package boofcv.alg.fiducial.qrcode;

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
public class TestQrPose3DUtils extends BoofStandardJUnit {
	@Test void getLandmarkByIndex() {
		QrCode qr = new QrCode();

		// this version has a locator pattern, but we don't care about those. sanity check to see
		// that it's ignored
		qr.version = 2;

		for (int i = 0; i < 4; i++) {
			qr.ppCorner.get(i).setTo(i, i + 1);
			qr.ppRight.get(i).setTo(4 + i, 4 + i + 1);
			qr.ppDown.get(i).setTo(8 + i, 8 + i + 1);
		}

		QrPose3DUtils alg = new QrPose3DUtils();
		List<PointIndex2D_F64> list = alg.getLandmarkByIndex(qr);

		assertEquals(12, list.size());
		for (int i = 0; i < 12; i++) {
			assertEquals(0, list.get(i).p.distance(i, i + 1), UtilEjml.TEST_F64);
		}
	}

	@Test void getLandmark2D3D() {

		QrCode qr = new QrCode();

		// try different versions. This will change the coordinate of points in the marker frame
		for (int v : new int[]{1, 2, 4, 10}) {
			int N = QrCode.totalModules(v);
			double w = 2.0*7/(double)N;
			qr.version = v;

			for (int i = 0; i < 4; i++) {
				qr.ppCorner.get(i).setTo(i, i + 1);
				qr.ppRight.get(i).setTo(4 + i, 4 + i + 1);
				qr.ppDown.get(i).setTo(8 + i, 8 + i + 1);
			}

			QrPose3DUtils alg = createAlg();

			List<Point2D3D> list = alg.getLandmark2D3D(qr);
			assertEquals(12, list.size());
			for (int i = 0; i < 12; i++) {
				// coordinate system is center (0,0) +x right +y up. normalized to have values -1 to 1
				Point3D_F64 X = list.get(i).location;

				assertTrue(X.x >= -1 && X.x <= 1);
				assertTrue(X.y >= -1 && X.y <= 1);
				assertEquals(0, X.z);
				assertEquals(0, list.get(i).observation.distance(0.1*i, 0.1*(i + 1)), UtilEjml.TEST_F64);
			}

			// check a few hand computed points
			assertEquals(0, list.get(0).location.distance(-1, 1, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(5).location.distance(1, 1, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(11).location.distance(-1, -1, 0), UtilEjml.TEST_F64);
			assertEquals(0, list.get(1).location.distance(-1 + w, 1, 0), UtilEjml.TEST_F64);
		}
	}

	/**
	 * Compare answer to what getLandmark2D3D() returns since the info is the same
	 */
	@Test void getLandmark3D() {
		QrCode qr = new QrCode();

		// try different versions. This will change the coordinate of points in the marker frame
		for (int v : new int[]{1, 2, 4, 10}) {
			qr.version = v;

			for (int i = 0; i < 4; i++) {
				qr.ppCorner.get(i).setTo(i, i + 1);
				qr.ppRight.get(i).setTo(4 + i, 4 + i + 1);
				qr.ppDown.get(i).setTo(8 + i, 8 + i + 1);
			}

			QrPose3DUtils alg = createAlg();

			List<Point2D3D> expected = alg.getLandmark2D3D(qr);
			List<Point3D_F64> found = alg.getLandmark3D(qr.version);

			assertEquals(expected.size(), found.size());
			for (int i = 0; i < expected.size(); i++) {
				assertEquals(0, expected.get(i).location.distance(found.get(i)), UtilEjml.TEST_F64);
			}
		}
	}

	private QrPose3DUtils createAlg() {
		QrPose3DUtils alg = new QrPose3DUtils();
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
