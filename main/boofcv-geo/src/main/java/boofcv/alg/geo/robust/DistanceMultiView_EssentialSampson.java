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

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.f.EssentialResidualSampson;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * <p>
 * Wrapper around {@link EssentialResidualSampson} for {@link DistanceFromModelMultiView}/
 * </p>
 *
 * @author Peter Abeles
 */
public class DistanceMultiView_EssentialSampson implements DistanceFromModelMultiView<DMatrixRMaj, AssociatedPair> {

	EssentialResidualSampson alg = new EssentialResidualSampson();

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		if (view == 0)
			alg.setCalibration1(intrinsic);
		else if (view == 1)
			alg.setCalibration2(intrinsic);
		else
			throw new RuntimeException("Unknown view");
	}

	@Override
	public int getNumberOfViews() {
		return 2;
	}

	@Override
	public void setModel( DMatrixRMaj E ) {
		alg.setModel(E);
	}

	@Override
	public double distance( AssociatedPair pt ) {
		return Math.abs(alg.computeResidual(pt));
	}

	@Override
	public void distances( List<AssociatedPair> pairs, double[] distance ) {
		for (int i = 0; i < pairs.size(); i++) {
			distance[i] = Math.abs(alg.computeResidual(pairs.get(i)));
		}
	}

	@Override
	public Class<AssociatedPair> getPointType() {
		return AssociatedPair.class;
	}

	@Override
	public Class<DMatrixRMaj> getModelType() {
		return DMatrixRMaj.class;
	}
}
