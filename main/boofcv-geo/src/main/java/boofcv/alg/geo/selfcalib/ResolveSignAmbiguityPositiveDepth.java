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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.AssociatedTuple;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * There's a sign ambiguity which flips the translation vector for several self calibration functions. This
 * uses the fact that for an observation to be seen it needs to be in front of the camera.
 *
 * @author Peter Abeles
 */
public class ResolveSignAmbiguityPositiveDepth implements VerbosePrint {
	/** Triangulation for n-view case */
	public TriangulateNViewsMetricH triangulator = FactoryMultiView.triangulateNViewMetricH(null);

	/** Indicates if the sign was changed */
	public boolean signChanged;
	/** Number of invalid in best hypothesis */
	public int bestInvalid;

	// 3D coordinate in view-1
	Point4D_F64 pointIn1 = new Point4D_F64();
	// 3D coordinate in view-I
	Point4D_F64 Xcam = new Point4D_F64();

	// precompute how to convert pixels into normalized image coordinates
	DogArray<PinholePtoN_F64> normalizers = new DogArray<>(PinholePtoN_F64::new);
	// Storage for normalized image coordinates
	DogArray<Point2D_F64> pixelNorms = new DogArray<>(Point2D_F64::new);
	DogArray<Se3_F64> worldToViews = new DogArray<>(Se3_F64::new);

	@Nullable PrintStream verbose;

	/**
	 * Processes the results and observations to fix the sign
	 *
	 * @param observations (input) Observations in pixels
	 * @param views (input/output) the current solution and modified to have the correct sign on output
	 */
	public void process( List<AssociatedTuple> observations, MetricCameras views ) {
		BoofMiscOps.checkTrue(views.intrinsics.size == views.motion_1_to_k.size + 1);
		BoofMiscOps.checkTrue(observations.size() > 0);

		final int numViews = views.intrinsics.size;
		final int numObs = observations.size();

		normalizers.resize(numViews);
		pixelNorms.resize(numViews);
		worldToViews.resize(numViews);

		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			normalizers.get(viewIdx).setK(views.intrinsics.get(viewIdx));
		}
		for (int viewIdx = 1; viewIdx < numViews; viewIdx++) {
			worldToViews.get(viewIdx).setTo(views.motion_1_to_k.get(viewIdx - 1));
		}

		signChanged = false;
		int best = -1;
		bestInvalid = Integer.MAX_VALUE;
		for (int trial = 0; trial < 2; trial++) {
			int foundInvalid = 0;
			for (int obsIdx = 0; obsIdx < numObs; obsIdx++) {
				// convert pixels into normalized image coordinates
				for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
					Point2D_F64 pixel = observations.get(obsIdx).get(viewIdx);
					normalizers.get(viewIdx).compute(pixel.x, pixel.y, pixelNorms.get(viewIdx));
				}

				// Find point in view-1 reference frame and check constraint
				if (!triangulator.triangulate(pixelNorms.toList(), worldToViews.toList(), pointIn1)) {
					foundInvalid += 2;
					continue;
				}

				if (PerspectiveOps.isBehindCamera(pointIn1))
					foundInvalid++;

				// Consistency check for remaining views
				for (int viewIdx = 1; viewIdx < numViews; viewIdx++) {
					SePointOps_F64.transform(worldToViews.get(viewIdx), pointIn1, Xcam);
					if (PerspectiveOps.isBehindCamera(Xcam))
						foundInvalid++;
				}
			}

			if (verbose != null) verbose.println("trial=" + trial + " invalid=" + foundInvalid + " obs=" + numObs);

			// flip to test other hypothesis next iteration
			for (int i = 1; i < worldToViews.size(); i++) {
				worldToViews.get(i).T.scale(-1);
			}

			// save best
			if (bestInvalid > foundInvalid) {
				bestInvalid = foundInvalid;
				best = trial;
			}
		}

		if (best == 1) {
			signChanged = true;
			for (int viewIdx = 0; viewIdx < views.motion_1_to_k.size; viewIdx++) {
				views.motion_1_to_k.get(viewIdx).T.scale(-1);
			}
		}

		if (verbose != null) verbose.println("best=" + best + " signChanged=" + signChanged);
	}

	/**
	 * Processes the results and observations to fix the sign
	 *
	 * @param observations (input) Observations in pixels
	 * @param result (input/output) the current solution and modified to have the correct sign on output
	 */
	public void process( List<AssociatedTriple> observations, MetricCameraTriple result ) {
		signChanged = false;
		int best = -1;
		bestInvalid = Integer.MAX_VALUE;

		normalizers.resize(3);
		pixelNorms.resize(3);
		worldToViews.resize(3);

		PinholePtoN_F64 normalize1 = normalizers.get(0);
		PinholePtoN_F64 normalize2 = normalizers.get(1);
		PinholePtoN_F64 normalize3 = normalizers.get(2);
		Point2D_F64 n1 = pixelNorms.get(0);
		Point2D_F64 n2 = pixelNorms.get(1);
		Point2D_F64 n3 = pixelNorms.get(2);

		worldToViews.get(1).setTo(result.view_1_to_2);
		worldToViews.get(2).setTo(result.view_1_to_3);

		normalize1.setK(result.view1);
		normalize2.setK(result.view2);
		normalize3.setK(result.view3);

		for (int trial = 0; trial < 2; trial++) {
			int foundInvalid = 0;
			for (int i = 0; i < observations.size(); i++) {
				AssociatedTriple ap = observations.get(i);

				// Convert from pixels to normalized image coordinates
				normalize1.compute(ap.p1.x, ap.p1.y, n1);
				normalize2.compute(ap.p2.x, ap.p2.y, n2);
				normalize2.compute(ap.p3.x, ap.p3.y, n3);

				// Find point in view-1 reference frame and check constraint
				triangulator.triangulate(pixelNorms.toList(), worldToViews.toList(), pointIn1);
				if (PerspectiveOps.isBehindCamera(pointIn1))
					foundInvalid++;

				// Find in view-2 and check +z constraint
				SePointOps_F64.transform(result.view_1_to_2, pointIn1, Xcam);
				if (PerspectiveOps.isBehindCamera(Xcam))
					foundInvalid++;

				// Find in view-3 and check +z constraint
				SePointOps_F64.transform(result.view_1_to_3, pointIn1, Xcam);
				if (PerspectiveOps.isBehindCamera(Xcam))
					foundInvalid++;
			}

			if (verbose != null)
				verbose.println("trial=" + trial + " invalid=" + foundInvalid + " obs=" + observations.size());

			// flip to test other hypothesis next iteration
			for (int i = 1; i < worldToViews.size(); i++) {
				worldToViews.get(i).T.scale(-1);
			}

			// save best
			if (bestInvalid > foundInvalid) {
				bestInvalid = foundInvalid;
				best = trial;
			}
		}

		if (best == 1) {
			signChanged = true;
			result.view_1_to_2.T.scale(-1);
			result.view_1_to_3.T.scale(-1);
		}

		if (verbose != null) verbose.println("best=" + best + " signChanged=" + signChanged);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
