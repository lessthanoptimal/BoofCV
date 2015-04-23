/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.DistanceModelStereoPixels;
import boofcv.alg.geo.NormalizedToPixelError;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes the error for a given camera motion from two calibrated views.  First a point
 * is triangulated from the two views and the motion.  Then the difference between
 * the observed and projected point is found at each view. Error is normalized pixel difference
 * squared.
 * </p>
 * <p>
 * error = &Delta;x<sub>1</sub><sup>2</sup> + &Delta;y<sub>1</sub><sup>2</sup> +
 * &Delta;x<sub>2</sub><sup>2</sup> + &Delta;y<sub>2</sub><sup>2</sup>
 * </p>
 *
 * <p>
 * Error units can be in either pixels<sup>2</sup> or unit less (normalized pixel coordinates).  To compute
 * the error in pixels pass in the correct intrinsic calibration parameters in the constructor.  Otherwise
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
public class DistanceSe3SymmetricSq implements DistanceModelStereoPixels<Se3_F64,AssociatedPair> {

	// transform from key frame to current frame
	private Se3_F64 keyToCurr;
	// triangulation algorithm
	private TriangulateTwoViewsCalibrated triangulate;
	// working storage
	private Point3D_F64 p = new Point3D_F64();

	// Used to compute error in pixels
	private NormalizedToPixelError errorCam1 = new NormalizedToPixelError();
	private NormalizedToPixelError errorCam2 = new NormalizedToPixelError();

	/**
	 * Configure distance calculation.
	 *
	 * @param triangulate Triangulates the intersection of two observations
	 */
	public DistanceSe3SymmetricSq(TriangulateTwoViewsCalibrated triangulate ,
								  double key_fx, double key_fy , double key_skew ,
								  double curr_fx, double curr_fy , double curr_skew ) {
		this.triangulate = triangulate;
		setIntrinsic(key_fx,key_fy,key_skew,curr_fx,curr_fy,curr_skew);
	}

	/**
	 * Configure distance calculation.
	 *
	 * @param triangulate Triangulates the intersection of two observations
	 */
	public DistanceSe3SymmetricSq(TriangulateTwoViewsCalibrated triangulate ) {
		this.triangulate = triangulate;
	}

	/**
	 * Specifies intrinsic parameters   See comment above about how to specify error units using
	 * intrinsic parameters.
	 *
	 * @param cam1_fx intrinsic parameter: focal length x for camera 1
	 * @param cam1_fy intrinsic parameter: focal length y for camera 1
	 * @param cam1_skew intrinsic parameter: skew for camera  1 (usually zero)
	 * @param cam2_fx intrinsic parameter: focal length x for camera 2
	 * @param cam2_fy intrinsic parameter: focal length y for camera 2
	 * @param cam2_skew intrinsic parameter: skew for camera 2 (usually zero)
	 */
	@Override
	public void setIntrinsic(double cam1_fx, double cam1_fy , double cam1_skew ,
							 double cam2_fx, double cam2_fy , double cam2_skew) {
		errorCam1.set(cam1_fx,cam1_fy,cam1_skew);
		errorCam2.set(cam2_fx,cam2_fy, cam2_skew);
	}

	@Override
	public void setModel(Se3_F64 keyToCurr) {
		this.keyToCurr = keyToCurr;
	}

	/**
	 * Computes the error given the motion model
	 *
	 * @param obs Observation in normalized pixel coordinates
	 * @return observation error
	 */
	@Override
	public double computeDistance(AssociatedPair obs) {

		// triangulate the point in 3D space
		triangulate.triangulate(obs.p1,obs.p2,keyToCurr,p);

		if( p.z < 0 )
			return Double.MAX_VALUE;

		// compute observational error in each view
		double error = errorCam1.errorSq(obs.p1.x,obs.p1.y,p.x/p.z,p.y/p.z);

		SePointOps_F64.transform(keyToCurr,p,p);
		if( p.z < 0 )
			return Double.MAX_VALUE;

		error += errorCam2.errorSq(obs.p2.x,obs.p2.y, p.x/p.z , p.y/p.z);

		return error;
	}

	@Override
	public void computeDistance(List<AssociatedPair> associatedPairs, double[] distance) {
		for( int i = 0; i < associatedPairs.size(); i++ ) {
			AssociatedPair obs = associatedPairs.get(i);
			distance[i] = computeDistance(obs);
		}
	}
}
