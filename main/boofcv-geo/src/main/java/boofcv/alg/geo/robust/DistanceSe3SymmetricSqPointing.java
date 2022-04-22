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

package boofcv.alg.geo.robust;

import boofcv.abst.geo.Triangulate2PointingMetricH;
import boofcv.alg.geo.DistanceFromModelMultiView2;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.PointingToProjectedPixelError;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.geo.AssociatedPair3D;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes the error for a given camera motion from two calibrated views. Same as {@link DistanceSe3SymmetricSq}
 * except that it takes in observations as pointing vectors.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceSe3SymmetricSqPointing implements DistanceFromModelMultiView2<Se3_F64, AssociatedPair3D> {

	// transform from key frame to current frame
	private Se3_F64 keyToCurr;
	// triangulation algorithm.
	private Triangulate2PointingMetricH triangulator;
	// working storage
	private Point4D_F64 p = new Point4D_F64();

	// Used to compute error in pixels
	private PointingToProjectedPixelError errorCam1 = new PointingToProjectedPixelError();
	private PointingToProjectedPixelError errorCam2 = new PointingToProjectedPixelError();

	/**
	 * Configure distance calculation.
	 *
	 * @param triangulator Triangulates the intersection of two observations
	 */
	public DistanceSe3SymmetricSqPointing( Triangulate2PointingMetricH triangulator ) {
		this.triangulator = triangulator;
	}

	@Override
	public void setModel( Se3_F64 keyToCurr ) {
		this.keyToCurr = keyToCurr;
	}

	/**
	 * Computes the error given the motion model
	 *
	 * @param obs Observation in normalized pixel coordinates
	 * @return observation error
	 */
	@Override
	public double distance( AssociatedPair3D obs ) {
		// triangulate the point in 3D space
		if (!triangulator.triangulate(obs.p1, obs.p2, keyToCurr, p))
			throw new RuntimeException("Triangulate failed. p1=" + obs.p1 + " p2=" + obs.p2);

		// TODO Adopt the behind camera check to cameras that have a FOV wider than 180 degrees.
		//     current code should cover 95% of the cases

		// If the point is at infinity then a different test needs to be used
		if (PerspectiveOps.isBehindCamera(p))
			return Double.MAX_VALUE;

		// compute observational error in each view
		double error = errorCam1.errorSq(obs.p1.x, obs.p1.y, obs.p1.z, p.x, p.y, p.z);

		SePointOps_F64.transform(keyToCurr, p, p);
		if (PerspectiveOps.isBehindCamera(p))
			return Double.MAX_VALUE;

		error += errorCam2.errorSq(obs.p2.x, obs.p2.y, obs.p2.z, p.x, p.y, p.z);

		return error;
	}

	@Override
	public void distances( List<AssociatedPair3D> associatedPairs, double[] distance ) {
		for (int i = 0; i < associatedPairs.size(); i++) {
			AssociatedPair3D obs = associatedPairs.get(i);
			distance[i] = distance(obs);
		}
	}

	@Override
	public Class<AssociatedPair3D> getPointType() {
		return AssociatedPair3D.class;
	}

	@Override
	public Class<Se3_F64> getModelType() {
		return Se3_F64.class;
	}

	@Override public void setDistortion( int view, Point3Transform2_F64 intrinsic ) {
		if (view == 0)
			errorCam1.setCamera(intrinsic);
		else if (view == 1)
			errorCam2.setCamera(intrinsic);
		else
			throw new IllegalArgumentException("View must be 0 or 1");
	}

	@Override
	public int getNumberOfViews() {
		return 2;
	}
}
