/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Wrapper that enables you to estimate an essential matrix while using a rigid body model
 *
 * @author Peter Abeles
 */
public class MmmvSe3ToEssential extends MmmvModelChanger<Se3_F64, DMatrixRMaj, AssociatedPair> {
	DMatrixRMaj E = new DMatrixRMaj(3, 3);

	public MmmvSe3ToEssential( ModelMatcherMultiview<Se3_F64, AssociatedPair> mmmv ) {
		super(mmmv);
	}

	@Override
	public DMatrixRMaj getModelParameters() {

		Se3_F64 found = mmmv.getModelParameters();

		MultiViewOps.createEssential(found.R, found.T, E);

		return E;
	}

	@Override
	public Class<DMatrixRMaj> getModelType() {
		return DMatrixRMaj.class;
	}
}
