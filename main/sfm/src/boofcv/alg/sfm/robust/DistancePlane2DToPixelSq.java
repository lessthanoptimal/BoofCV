/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.alg.geo.NormalizedToPixelError;
import boofcv.alg.sfm.overhead.CameraPlaneProjection;
import boofcv.struct.sfm.PlanePtPixel;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes the difference between a predicted observation and the actual observation.  The prediction is done
 * by applying a transform on a point in on a 2D plane then rendering it onto the camera.
 *
 * @author Peter Abeles
 */
public class DistancePlane2DToPixelSq implements DistanceFromModel<Se2_F64,PlanePtPixel> {

	// motion from key frame to current frame in plane 2D reference frame
	private Se2_F64 keyToCurr;

	// predicted location of point on plane in current ref frame in 2D
	private Point2D_F64 curr2D = new Point2D_F64();

	// normalized image coordinates of predicted position
	private Point2D_F64 normalizedPred = new Point2D_F64();

	// code for projection to/from plane
	private CameraPlaneProjection planeProjection = new CameraPlaneProjection();

	// given observations in normalized image coordinates, compute the error in pixels
	private NormalizedToPixelError errorCamera = new NormalizedToPixelError();

	/**
	 * Specify extrinsic camera properties
	 * @param planeToCamera Transform from plane to camera reference frame
	 */
	public void setExtrinsic(Se3_F64 planeToCamera) {
		planeProjection.setPlaneToCamera(planeToCamera, false);
	}

	/**
	 * Specify intrinsic camera properties
	 * @param fx focal length x
	 * @param fy focal length y
	 * @param skew camera skew
	 */
	public void setIntrinsic( double fx, double fy, double skew ) {
		errorCamera.set(fx, fy, skew);
	}

	@Override
	public void setModel(Se2_F64 keyToCurr) {
		this.keyToCurr = keyToCurr;
	}

	@Override
	public double computeDistance(PlanePtPixel sample ) {

		// apply transform from key frame to current frame
		SePointOps_F64.transform(keyToCurr, sample.planeKey, curr2D);

		// project plane to normalized
		if( !planeProjection.planeToNormalized(curr2D.x, curr2D.y, normalizedPred) )
			return Double.MAX_VALUE;

		// Euclidean pixel error squared error
		return errorCamera.errorSq(sample.normalizedCurr, normalizedPred);
	}

	@Override
	public void computeDistance(List<PlanePtPixel> samples, double[] distance) {
		for( int i = 0; i < samples.size(); i++ ) {
			distance[i] = computeDistance(samples.get(i));
		}
	}
}
