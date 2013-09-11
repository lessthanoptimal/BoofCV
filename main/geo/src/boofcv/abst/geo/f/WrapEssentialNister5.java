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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EstimateNofEpipolar;
import boofcv.alg.geo.f.EssentialNister5;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around either {@link boofcv.alg.geo.f.EssentialNister5} for {@link boofcv.abst.geo.EstimateNofEpipolar}.
 *
 * @author Peter Abeles
 */
public class WrapEssentialNister5 implements EstimateNofEpipolar {
	EssentialNister5 alg;

	public WrapEssentialNister5() {
		alg = new EssentialNister5();
	}

	@Override
	public boolean process(List<AssociatedPair> points, FastQueue<DenseMatrix64F> estimatedModels) {
		if( !alg.process(points,estimatedModels) )
			return false;

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 5;
	}
}
