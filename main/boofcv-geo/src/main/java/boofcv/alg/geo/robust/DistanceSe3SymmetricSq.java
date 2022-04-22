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

import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.NormalizedToPinholePixelError;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes the error for a given camera motion from two calibrated views. First a point
 * is triangulated in homogenous coordinates from the two views and the motion. Then the difference between
 * the observed and projected point is found at each view. Error is normalized pixel difference
 * squared.
 * </p>
 * <p>
 * error = &Delta;x<sub>1</sub><sup>2</sup> + &Delta;y<sub>1</sub><sup>2</sup> +
 * &Delta;x<sub>2</sub><sup>2</sup> + &Delta;y<sub>2</sub><sup>2</sup>
 * </p>
 *
 * <p>Homogenous coordinates are used so that pure/nearly pure rotation can be handled. Points will be at infinity.</p>
 *
 * <p>
 * Error units can be in either pixels<sup>2</sup> or unit less (normalized pixel coordinates). To compute
 * the error in pixels pass in the correct intrinsic calibration parameters in the constructor. Otherwise
 * pass in fx=1.fy=1,skew=0 for normalized.
 * </p>
 *
 * <p>
 * NOTE: If a point does not pass the positive depth constraint then a very large error is returned.
 * </p>
 *
 * <p>
 * NOTE: The provided transform must be from the key frame into the current frame.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceSe3SymmetricSq implements DistanceFromModelMultiView<Se3_F64, AssociatedPair> {

	// transform from key frame to current frame
	private Se3_F64 keyToCurr;
	// triangulation algorithm.
	private Triangulate2ViewsMetricH triangulator;
	// working storage
	private Point4D_F64 p = new Point4D_F64();

	// Used to compute error in pixels
	private NormalizedToPinholePixelError errorCam1 = new NormalizedToPinholePixelError();
	private NormalizedToPinholePixelError errorCam2 = new NormalizedToPinholePixelError();

	/**
	 * Configure distance calculation.
	 *
	 * @param triangulator Triangulates the intersection of two observations
	 */
	public DistanceSe3SymmetricSq( Triangulate2ViewsMetricH triangulator ) {
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
	public double distance( AssociatedPair obs ) {
		// triangulate the point in 3D space
		if (!triangulator.triangulate(obs.p1, obs.p2, keyToCurr, p))
			throw new RuntimeException("Triangulate failed. p1=" + obs.p1 + " p2=" + obs.p2);

		// If the point is at infinity then a different test needs to be used
		if (PerspectiveOps.isBehindCamera(p))
			return Double.MAX_VALUE;

		// compute observational error in each view
		double error = errorCam1.errorSq(obs.p1.x, obs.p1.y, p.x/p.z, p.y/p.z);

		SePointOps_F64.transform(keyToCurr, p, p);
		if (PerspectiveOps.isBehindCamera(p))
			return Double.MAX_VALUE;

		error += errorCam2.errorSq(obs.p2.x, obs.p2.y, p.x/p.z, p.y/p.z);

		return error;
	}

	@Override
	public void distances( List<AssociatedPair> associatedPairs, double[] distance ) {
		for (int i = 0; i < associatedPairs.size(); i++) {
			AssociatedPair obs = associatedPairs.get(i);
			distance[i] = distance(obs);
		}
	}

	@Override
	public Class<AssociatedPair> getPointType() {
		return AssociatedPair.class;
	}

	@Override
	public Class<Se3_F64> getModelType() {
		return Se3_F64.class;
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		if (view == 0)
			errorCam1.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		else if (view == 1)
			errorCam2.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		else
			throw new IllegalArgumentException("View must be 0 or 1");
	}

	@Override
	public int getNumberOfViews() {
		return 2;
	}
}
