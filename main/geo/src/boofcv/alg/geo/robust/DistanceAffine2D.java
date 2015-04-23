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

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;


/**
 * Applies an affine transformation to the associated pair and computes the euclidean distance
 * between their locations.  The transform is applied to the "keyLoc".
 *
 * @author Peter Abeles
 */
public class DistanceAffine2D implements DistanceFromModel<Affine2D_F64,AssociatedPair> {

	Affine2D_F64 model;
	Point2D_F64 expected = new Point2D_F64();

	@Override
	public void setModel(Affine2D_F64 model ) {
		this.model = model;
	}

	@Override
	public double computeDistance(AssociatedPair pt) {
		AffinePointOps_F64.transform(model, pt.p1, expected);

		return expected.distance(pt.p2);
	}

	@Override
	public void computeDistance(List<AssociatedPair> points, double[] distance) {
		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);
			AffinePointOps_F64.transform(model,p.p1,expected);

			distance[i] = expected.distance(p.p2);
		}
	}
}
