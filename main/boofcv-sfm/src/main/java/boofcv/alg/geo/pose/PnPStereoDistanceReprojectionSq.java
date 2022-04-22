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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.NormalizedToPinholePixelError;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes sum of reprojection error squared in pixels for a pair of stereo observations. If the point
 * is behind either the left or right camera and can't be viewed then Double.MAX_VALUE is returned.<br>
 * <br>
 * error = dx0^2 + dy0^2 + dx1^2 + dy1^2<br>
 * <br>
 * where dx0 = residual along x-axis in image 0
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PnPStereoDistanceReprojectionSq implements DistanceFromModelMultiView<Se3_F64, Stereo2D3D> {

	// transform from world to left camera. Model being tested.
	private Se3_F64 worldToLeft;

	// storage for point in camera frame
	private final Point3D_F64 X = new Point3D_F64();

	// transform from left to right camera. Assumed to be known.
	private Se3_F64 leftToRight = new Se3_F64();
	// computes the error in units of pixels. Assumed to be known
	private NormalizedToPinholePixelError leftPixelError = new NormalizedToPinholePixelError();
	private NormalizedToPinholePixelError rightPixelError = new NormalizedToPinholePixelError();

	public PnPStereoDistanceReprojectionSq( NormalizedToPinholePixelError leftPixelError,
											NormalizedToPinholePixelError rightPixelError,
											Se3_F64 leftToRight ) {
		this.leftPixelError = leftPixelError;
		this.rightPixelError = rightPixelError;
		this.leftToRight = leftToRight;
	}

	public PnPStereoDistanceReprojectionSq() {}

	public void setLeftToRight( Se3_F64 leftToRight ) {
		this.leftToRight.setTo(leftToRight);
	}

	@Override
	public void setModel( Se3_F64 worldToLeft ) {
		this.worldToLeft = worldToLeft;
	}

	@Override
	public double distance( Stereo2D3D pt ) {
		// Compute error in left camera first.
		// Project observation into the image plane
		SePointOps_F64.transform(worldToLeft, pt.location, X);

		// very large error if behind the camera
		if (X.z <= 0)
			return Double.MAX_VALUE;

		Point2D_F64 p = pt.leftObs;
		double errorLeft = leftPixelError.errorSq(X.x/X.z, X.y/X.z, p.x, p.y);

		// point from left camera to right camera reference frame
		SePointOps_F64.transform(leftToRight, X, X);

		// now the right error
		if (X.z <= 0)
			return Double.MAX_VALUE;

		p = pt.rightObs;
		double errorRight = rightPixelError.errorSq(X.x/X.z, X.y/X.z, p.x, p.y);

		return errorLeft + errorRight;
	}

	@Override
	public void distances( List<Stereo2D3D> observations, double[] distance ) {
		for (int i = 0; i < observations.size(); i++)
			distance[i] = distance(observations.get(i));
	}

	@Override
	public Class<Stereo2D3D> getPointType() {
		return Stereo2D3D.class;
	}

	@Override
	public Class<Se3_F64> getModelType() {
		return Se3_F64.class;
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		if (view == 0)
			leftPixelError.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		else if (view == 1)
			rightPixelError.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		else
			throw new IllegalArgumentException("View must be 0 or 1");
	}

	@Override
	public int getNumberOfViews() {
		return 2;
	}

	public void setStereoParameters( StereoParameters param ) {
		param.right_to_left.invert(leftToRight);
		setIntrinsic(0, param.left);
		setIntrinsic(1, param.right);
	}

	/**
	 * Creates a child which references the intrinsics but is otherwise decoupled
	 */
	public DistanceFromModelMultiView<Se3_F64, Stereo2D3D> newConcurrentChild() {
		return new PnPStereoDistanceReprojectionSq(leftPixelError, rightPixelError, leftToRight);
	}
}
