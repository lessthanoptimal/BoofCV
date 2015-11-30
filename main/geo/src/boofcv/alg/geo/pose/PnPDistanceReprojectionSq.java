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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.NormalizedToPixelError;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>Computes the reprojection error squared for a given motion and {@link boofcv.struct.geo.Point2D3D}.
 * If the intrinsic parameters are provided then the error will be computed in pixels.   Observations are
 * assumed to be in normalized image coordinates.</p>
 *
 * <center>error = (x'-x)^2 + (y' - y)^2</center>
 *
 * <p>where (x,y) is the observed point location and (x',y') is the reprojected point from the 3D coordinate
 * and coordinate transformation.</p>
 *
 * @author Peter Abeles
 */
public class PnPDistanceReprojectionSq implements DistanceModelMonoPixels<Se3_F64,Point2D3D> {

	// transform from world to camera
	private Se3_F64 worldToCamera;

	// storage for point in camera frame
	private Point3D_F64 X = new Point3D_F64();

	// computes the error in units of pixels
	private NormalizedToPixelError pixelError;

	public PnPDistanceReprojectionSq() {
		this(1,1,0);
	}

	/**
	 * Computes reprojection error in pixels when provided to camera's intrinsic calibration.
	 *
	 * @param fx focal length x
	 * @param fy focal length y
	 * @param skew pixel skew
	 */
	public PnPDistanceReprojectionSq(double fx, double fy, double skew) {
		setIntrinsic(fx,fy,skew);
	}

	@Override
	public void setIntrinsic(double fx, double fy, double skew) {
		pixelError = new NormalizedToPixelError(fx,fy,skew);
	}

	@Override
	public void setModel(Se3_F64 worldToCamera) {
		this.worldToCamera = worldToCamera;
	}

	@Override
	public double computeDistance(Point2D3D pt) {
		// compute point location in camera frame
		SePointOps_F64.transform(worldToCamera,pt.location,X);

		// very large error if behind the camera
		if( X.z <= 0 )
			return Double.MAX_VALUE;

		Point2D_F64 p = pt.getObservation();

		return pixelError.errorSq(X.x/X.z,X.y/X.z,p.x,p.y);
	}

	@Override
	public void computeDistance(List<Point2D3D> observations, double[] distance) {
		for( int i = 0; i < observations.size(); i++ )
			distance[i] = computeDistance(observations.get(i));
	}
}
