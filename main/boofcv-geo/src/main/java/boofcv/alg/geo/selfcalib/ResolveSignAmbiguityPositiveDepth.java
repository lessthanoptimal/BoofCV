/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * There's a sign ambiguity which flips the translation vector for several self calibration functions. This
 * uses the fact that for an observation to be seen it needs to be in front of the camera.
 *
 * @author Peter Abeles
 */
public class ResolveSignAmbiguityPositiveDepth {
	Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(null);
	// Storage for normalized image coordinates
	Point2D_F64 n1 = new Point2D_F64();
	Point2D_F64 n2 = new Point2D_F64();

	// 3D coordinate in view-1
	Point3D_F64 pointIn1 = new Point3D_F64();
	// 3D coordinate in view-I
	Point3D_F64 Xcam = new Point3D_F64();

	// precompute how to convert pixels into normalized image coordinates
	PinholePtoN_F64 normalize1 = new PinholePtoN_F64();
	PinholePtoN_F64 normalize2 = new PinholePtoN_F64();

	/**
	 * Processes the results and observations to fix the sign
	 *
	 * @param observations (input) Observations in pixels
	 * @param result (input/output) the current solution and modified to have the correct sign on output
	 */
	public void process(List<AssociatedTriple> observations, MetricCameraTriple result ) {
		int best = -1;
		int bestInvalid = Integer.MAX_VALUE;

		normalize1.set(result.view1);
		normalize2.set(result.view2);

		for (int trial = 0; trial < 2; trial++) {
			int foundInvalid = 0;
			for (int i = 0; i < observations.size(); i++) {
				AssociatedTriple ap = observations.get(i);

				// Convert from pixels to normalized image coordinates
				normalize1.compute(ap.p1.x, ap.p1.y, n1);
				normalize2.compute(ap.p2.x, ap.p2.y, n2);

				// Find point in view-1 reference frame and check constraint
				triangulate.triangulate(n1, n2, result.view_1_to_2, pointIn1);
				if (pointIn1.z < 0)
					foundInvalid++;

				// Find in view-2 and check +z constraint
				SePointOps_F64.transform(result.view_1_to_2, pointIn1, Xcam);
				if (Xcam.z < 0)
					foundInvalid++;
			}

			// flip to test other hypothesis next iteration
			result.view_1_to_2.T.scale(-1);
			result.view_1_to_3.T.scale(-1);

			// save best
			if( bestInvalid > foundInvalid ) {
				bestInvalid = foundInvalid;
				best = trial;
			}
		}

		if( best == 1 ) {
			result.view_1_to_2.T.scale(-1);
			result.view_1_to_3.T.scale(-1);
		}
	}
}
