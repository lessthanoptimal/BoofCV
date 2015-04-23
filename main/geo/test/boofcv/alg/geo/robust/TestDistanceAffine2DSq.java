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


/**
 * @author Peter Abeles
 */
public class TestDistanceAffine2DSq extends TestDistanceAffine2D {

	@Override
	public DistanceFromModel<Affine2D_F64, AssociatedPair> create() {
		return new DistanceAffine2DSq();
	}

	@Override
	public double distance(Affine2D_F64 affine, AssociatedPair associatedPair) {

		Point2D_F64 result = new Point2D_F64();

		AffinePointOps_F64.transform(affine, associatedPair.p1, result);

		return result.distance2(associatedPair.p2);
	}
}
