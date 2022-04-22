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
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * <p>Computes the reprojection error squared for a given motion and {@link boofcv.struct.geo.Point2D3D}.
 * If the intrinsic parameters are provided then the error will be computed in pixels. Observations are
 * assumed to be in normalized image coordinates.</p>
 *
 * <center>error = (x'-x)^2 + (y' - y)^2</center>
 *
 * <p>where (x,y) is the observed point location and (x',y') is the reprojected point from the 3D coordinate
 * and coordinate transformation.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PnPDistanceReprojectionSq implements DistanceFromModelMultiView<Se3_F64, Point2D3D> {

	// transform from world to camera
	private Se3_F64 worldToCamera;

	// storage for point in camera frame
	private final Point3D_F64 X = new Point3D_F64();

	// computes the error in units of pixels
	private NormalizedToPinholePixelError pixelError = new NormalizedToPinholePixelError(1, 1, 0);

	public PnPDistanceReprojectionSq( NormalizedToPinholePixelError pixelError ) {
		this.pixelError = pixelError;
	}

	public PnPDistanceReprojectionSq() {}

	@Override
	public void setModel( Se3_F64 worldToCamera ) {
		this.worldToCamera = worldToCamera;
	}

	@Override
	public double distance( Point2D3D pt ) {
		// compute point location in camera frame
		SePointOps_F64.transform(worldToCamera, pt.location, X);

		// very large error if behind the camera
		if (X.z <= 0)
			return Double.MAX_VALUE;

		Point2D_F64 p = pt.getObservation();

		return pixelError.errorSq(X.x/X.z, X.y/X.z, p.x, p.y);
	}

	@Override
	public void distances( List<Point2D3D> observations, double[] distance ) {
		for (int i = 0; i < observations.size(); i++)
			distance[i] = distance(observations.get(i));
	}

	@Override
	public Class<Point2D3D> getPointType() {
		return Point2D3D.class;
	}

	@Override
	public Class<Se3_F64> getModelType() {
		return Se3_F64.class;
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		pixelError.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
	}

	@Override
	public int getNumberOfViews() {
		return 1;
	}

	/**
	 * Creates a child which references the intrinsics but is otherwise decoupled
	 */
	public DistanceFromModelMultiView<Se3_F64, Point2D3D> newConcurrentChild() {
		return new PnPDistanceReprojectionSq(pixelError);
	}
}
