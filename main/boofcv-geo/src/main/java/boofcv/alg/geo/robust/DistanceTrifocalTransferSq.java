/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.trifocal.TrifocalTransfer;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes transfer error squared from views 1 + 2 to 3 and 1 + 3 to 2.
 *
 * @author Peter Abeles
 */
public class DistanceTrifocalTransferSq implements DistanceFromModel<TrifocalTensor, AssociatedTriple> {
	TrifocalTransfer transfer = new TrifocalTransfer();

	// transferred point in homogenous coordinates
	Point3D_F64 c = new Point3D_F64();

	@Override
	public void setModel( TrifocalTensor trifocalTensor ) {
		transfer.setTrifocal(trifocalTensor);
	}

	@Override
	public double distance( AssociatedTriple pt ) {
		transfer.transfer_1_to_3(pt.p1.x, pt.p1.y, pt.p2.x, pt.p2.y, c);
		double error = UtilPoint2D_F64.distanceSq(c.x/c.z, c.y/c.z, pt.p3.x, pt.p3.y);

		transfer.transfer_1_to_2(pt.p1.x, pt.p1.y, pt.p3.x, pt.p3.y, c);
		error += UtilPoint2D_F64.distanceSq(c.x/c.z, c.y/c.z, pt.p2.x, pt.p2.y);
		return error;
	}

	@Override
	public void distances( List<AssociatedTriple> list, double[] distance ) {
		for (int i = 0; i < list.size(); i++) {
			distance[i] = distance(list.get(i));
		}
	}

	@Override
	public Class<AssociatedTriple> getPointType() {
		return AssociatedTriple.class;
	}

	@Override
	public Class<TrifocalTensor> getModelType() {
		return TrifocalTensor.class;
	}
}
