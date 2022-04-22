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

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.NormalizedToPinholePixelError;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;

import java.util.List;

/**
 * <p>
 * Computes the Euclidean error squared between 'p1' and 'p2' after projecting 'p1' into image 2. Input
 * points must be in normalized image coordinates, but the output error will be in pixel coordinates.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceHomographyCalibratedSq implements DistanceFromModelMultiView<Homography2D_F64, AssociatedPair> {
	Homography2D_F64 model;
	private Point2D_F64 expected = new Point2D_F64();
	private NormalizedToPinholePixelError errorCam2 = new NormalizedToPinholePixelError();

	@Override
	public void setModel( Homography2D_F64 model ) {
		this.model = model;
	}

	@Override
	public double distance( AssociatedPair pt ) {
		HomographyPointOps_F64.transform(model, pt.p1, expected);

		return errorCam2.errorSq(expected, pt.p2);
	}

	@Override
	public void distances( List<AssociatedPair> points, double[] distance ) {
		for (int i = 0; i < points.size(); i++) {
			AssociatedPair p = points.get(i);
			HomographyPointOps_F64.transform(model, p.p1, expected);
			distance[i] = errorCam2.errorSq(expected, p.p2);
		}
	}

	@Override
	public Class<AssociatedPair> getPointType() {
		return AssociatedPair.class;
	}

	@Override
	public Class<Homography2D_F64> getModelType() {
		return Homography2D_F64.class;
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		if (view == 1)
			errorCam2.setTo(intrinsic.fx, intrinsic.fy, intrinsic.skew);
		else if (view != 0)
			throw new IllegalArgumentException("View must be 0 or 1");
	}

	@Override
	public int getNumberOfViews() {
		return 2;
	}
}
