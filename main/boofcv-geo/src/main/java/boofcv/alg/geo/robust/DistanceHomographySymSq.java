/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;

import java.util.List;

/// Computes the Euclidean symmetric error squared between 'p1' and 'p2' after projecting 'p1' into image 2 then does
/// the reverse and computes the average. The average is returned to keep the magnitude consistent between
/// [DistanceHomographySq].
///
/// error = {@code 0.5*([u' - u]'*[u' - u] + [v' - v]'*[v' - v])} where u and v are the observations in each image,
/// respectively. {@code u'} and {@code v'} are the predicted locations using the other image.
@SuppressWarnings({"NullAway.Init"})
public class DistanceHomographySymSq implements DistanceFromModel<Homography2D_F64, AssociatedPair> {

	Homography2D_F64 H = new Homography2D_F64();
	Homography2D_F64 Hinv = new Homography2D_F64();
	Point2D_F64 expected = new Point2D_F64();

	public void setModel( DMatrixRMaj H ) {
		if (this.H == null)
			this.H = new Homography2D_F64();
		UtilHomography_F64.convert(H, this.H);
		CommonOps_DDF3.invert(this.H, Hinv);
	}

	@Override
	public void setModel( Homography2D_F64 model ) {
		this.H.setTo(model);
		CommonOps_DDF3.invert(H, Hinv);
	}

	@Override
	public double distance( AssociatedPair pt ) {
		HomographyPointOps_F64.transform(H, pt.p1, expected);
		double error = expected.distance2(pt.p2);
		HomographyPointOps_F64.transform(Hinv, pt.p2, expected);
		error += expected.distance2(pt.p1);

		return error / 2.0;
	}

	@Override
	public void distances( List<AssociatedPair> points, double[] distance ) {
		for (int i = 0; i < points.size(); i++) {
			distance[i] = distance(points.get(i));
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
}
