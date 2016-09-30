/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.NormalizedToPixelError;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * <p>
 * Computes sum of reprojection error squared in pixels for a pair of stereo observations.  If the point
 * is behind either the left or right camera and can't be viewed then Double.MAX_VALUE is returned.<br>
 * <br>
 * error = dx0^2 + dy0^2 + dx1^2 + dy1^2<br>
 * <br>
 * where dx0 = residual along x-axis in image 0
 * </p>
 *
 * @author Peter Abeles
 */
public class PnPStereoDistanceReprojectionSq implements DistanceFromModel<Se3_F64,Stereo2D3D> {

	// transform from world to left camera
	private Se3_F64 worldToLeft;
	// transform from left to right camera
	private Se3_F64 leftToRight;

	// storage for point in camera frame
	private Point3D_F64 X = new Point3D_F64();

	// computes the error in units of pixels
	private NormalizedToPixelError leftPixelError;
	private NormalizedToPixelError rightPixelError;

	public void setStereoParameters(StereoParameters param)
	{
		this.leftToRight = param.getRightToLeft().invert(null);

		CameraPinholeRadial left = param.left;
		CameraPinholeRadial right = param.right;

		leftPixelError = new NormalizedToPixelError(left.fx,left.fy,left.skew);
		rightPixelError = new NormalizedToPixelError(right.fx,right.fy,right.skew);
	}

	@Override
	public void setModel(Se3_F64 worldToLeft) {
		this.worldToLeft = worldToLeft;
	}

	@Override
	public double computeDistance(Stereo2D3D pt) {
		// Compute error in left camera first.
		// Project observation into the image plane
		SePointOps_F64.transform(worldToLeft, pt.location, X);

		// very large error if behind the camera
		if( X.z <= 0 )
			return Double.MAX_VALUE;

		Point2D_F64 p = pt.leftObs;
		double errorLeft =  leftPixelError.errorSq(X.x/X.z,X.y/X.z,p.x,p.y);

		// point from left camera to right camera reference frame
		SePointOps_F64.transform(leftToRight, X, X);

		// now the right error
		if( X.z <= 0 )
			return Double.MAX_VALUE;

		p = pt.rightObs;
		double errorRight = rightPixelError.errorSq(X.x/X.z,X.y/X.z,p.x,p.y);

		return errorLeft + errorRight;
	}

	@Override
	public void computeDistance(List<Stereo2D3D> observations, double[] distance) {
		for( int i = 0; i < observations.size(); i++ )
			distance[i] = computeDistance(observations.get(i));
	}
}
