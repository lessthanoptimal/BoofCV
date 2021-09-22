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

package boofcv.alg.geo.pose;

import boofcv.abst.geo.EstimateNofPnP;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPnPStereoEstimator extends CommonStereoMotionNPoint {

	@Test void perfectData() {
		perfectData(0);
		perfectData(1);
		perfectData(2);
	}

	private void perfectData( int numExtra ) {
		EstimateNofPnP pnp = FactoryMultiView.pnp_N(EnumPNP.P3P_FINSTERWALDER, -1);
		var distanceLeft = new PnPDistanceReprojectionSq();
		var distanceRight = new PnPDistanceReprojectionSq();

		PnPStereoEstimator alg = new PnPStereoEstimator(pnp, distanceLeft, distanceRight, numExtra);

		Se3_F64 expected = new Se3_F64();
		expected.getR().setTo(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		expected.getT().setTo(0.2, -0.1, 0.01);

		generateScene(alg.getMinimumPoints(), expected, false);

		Se3_F64 found = new Se3_F64();

		alg.setLeftToRight(leftToRight);
		assertTrue(alg.process(pointPose, found));

		assertTrue(MatrixFeatures_DDRM.isIdentical(expected.getR(), found.getR(), 1e-8));
		assertTrue(found.getT().isIdentical(expected.getT(), 1e-8));
	}
}
