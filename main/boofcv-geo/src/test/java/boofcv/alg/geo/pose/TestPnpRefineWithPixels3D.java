/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.geo.Point2D3D;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPnpRefineWithPixels3D extends BoofStandardJUnit {

	CameraPinholeBrown camera = new CameraPinholeBrown().fsetK(500, 490, 0, 470, 490, 900, 860);

	@Test void general() {
		var alg = new PnPRefineWithPixels3D();
		List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-0.5, 0.5, -0.5, 0.5, 2.0, 2.5, 30, rand);

		checkSolution(alg, worldPts);
	}

	@Test void planar() {
		var alg = new PnPRefineWithPixels3D();
		List<Point3D_F64> worldPts = CommonHomographyChecks.createRandomPlane(rand, 2.2, 30);
		checkSolution(alg, worldPts);
	}

	/** Make sure it still works when you call it multiple times */
	@Test void multipleCalls() {
		var alg = new PnPRefineWithPixels3D();
		for (int i = 0; i < 3; i++) {
			List<Point3D_F64> worldPts = GeoTestingOps.randomPoints_F64(-0.5, 0.5, -0.5, 0.5, 2.0, 2.5, 30, rand);
			checkSolution(alg, worldPts);
		}
	}

	private void checkSolution( PnPRefineWithPixels3D alg, List<Point3D_F64> worldPts ) {
		var worldToView = SpecialEuclideanOps_F64.eulerXyz(0.2, -0.15 + rand.nextGaussian()*0.1, 0, 0.05, -0.02, 0.1, null);

		var w2p = new WorldToCameraToPixel();
		w2p.configure(camera, worldToView);

		var points = new ArrayList<Point2D3D>();
		for (int i = 0; i < worldPts.size(); i++) {
			var p = new Point2D3D();
			p.location.setTo(worldPts.get(i));
			w2p.transform(p.location, p.observation);
			points.add(p);
		}

		var adjusted = new Se3_F64().setTo(worldToView);
		adjusted.T.x += 0.3;
		var found = new Se3_F64();


		alg.setCamera(new BundlePinholeBrown(true, false, -1).setTo(camera), null);
		assertTrue(alg.refine(points, adjusted, found));

		assertTrue(SpecialEuclideanOps_F64.isIdentical(worldToView, found, 1e-4, 1e-4));
	}


}
