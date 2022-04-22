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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EstimateNofEpipolarPointing;
import boofcv.alg.geo.f.EssentialNister5;
import boofcv.struct.geo.AssociatedPair3D;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Wrapper around either {@link EssentialNister5} for {@link EstimateNofEpipolarPointing}.
 *
 * @author Peter Abeles
 */
public class WrapEssentialNister5Pointing implements EstimateNofEpipolarPointing {
	EssentialNister5 alg;

	public WrapEssentialNister5Pointing() {
		alg = new EssentialNister5();
	}

	@Override
	public boolean process( List<AssociatedPair3D> points, DogArray<DMatrixRMaj> estimatedModels ) {
		if (!alg.processPointing(points, estimatedModels))
			return false;

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 5;
	}
}
