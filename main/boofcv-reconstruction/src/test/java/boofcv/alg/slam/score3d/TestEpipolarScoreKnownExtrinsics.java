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

package boofcv.alg.slam.score3d;

import boofcv.alg.distort.SphereToNarrowPixel_F64;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEpipolarScoreKnownExtrinsics extends BoofStandardJUnit {
	private final CameraPinholeBrown intrinsic1 = new CameraPinholeBrown(400, 400, 0, 500, 500, 1000, 1000).fsetRadial(0.0, 0.0);
	private final CameraPinholeBrown intrinsic2 = new CameraPinholeBrown(600, 600, 0, 600, 600, 1000, 1000).fsetRadial(0.0, 0.0);
	private final int numPoints = 100;
	private final List<Point3D_F64> feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, numPoints, rand);

	/**
	 * Compute the score for different scenarios and see if there is a higher score when there is more motion
	 */
	@Test void largerMotionHigherScore() {
		// run scenarios with different amounts of translation between the views
		double score1 = scoreScenario(SpecialEuclideanOps_F64.eulerXyz(0.5, 0, 0, 0, 0, 0, null), true);
		double score2 = scoreScenario(SpecialEuclideanOps_F64.eulerXyz(0.1, 0, 0, 0, 0, 0, null), true);
		double score3 = scoreScenario(SpecialEuclideanOps_F64.eulerXyz(0.0, 0, 0, 0, 0, 0, null), false);

		assertTrue(score1 > score2);
		assertTrue(score2 > score3);
	}

	private double scoreScenario( Se3_F64 a_to_b, boolean is3D ) {
		List<Point3D_F64> observations1 = new ArrayList<>();
		List<Point3D_F64> observations2 = new ArrayList<>();
		List<AssociatedIndex> pairs = new ArrayList<>();

		for (int i = 0; i < feats3D.size(); i++) {
			Point3D_F64 X1 = feats3D.get(i).copy();
			Point3D_F64 X2 = new Point3D_F64();

			SePointOps_F64.transform(a_to_b, X1, X2);

			// Add a tiny bit of noise
			X1.z += rand.nextGaussian()*0.002;
			X2.z += rand.nextGaussian()*0.002;

			// Create pointing vector observations from the location of the feature in each reference frame
			X1.divideIP(X1.norm());
			X2.divideIP(X2.norm());

			observations1.add(X1);
			observations2.add(X2);

			pairs.add(new AssociatedIndex(i, i));
		}

		Point3Transform2_F64 lens1 = new SphereToNarrowPixel_F64(LensDistortionFactory.
				narrow(intrinsic1).distort_F64(false, true));
		Point3Transform2_F64 lens2 = new SphereToNarrowPixel_F64(LensDistortionFactory.
				narrow(intrinsic1).distort_F64(false, true));

		var inliersIdx = new DogArray_I32();
		var alg = new EpipolarScoreKnownExtrinsics();
//		alg.maxReprojectionError.setRelative(0.0001, 1);
		alg.process(intrinsic1.getDimension(null), intrinsic2.getDimension(null),
				lens1, lens2, observations1, observations2, pairs, a_to_b, inliersIdx);

		if (is3D)
			assertEquals(numPoints, inliersIdx.size);
		assertEquals(is3D, alg.is3D);
		return alg.getScore();
	}
}
